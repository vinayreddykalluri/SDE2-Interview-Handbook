# Relational Modeling, Keys, Constraints, and Normalization

## Begin with facts, not Java classes

A relation represents facts with a stable meaning. A row should have an identity, columns should describe that fact, and constraints should reject states the business says are impossible.

For “a customer can place many orders,” the foreign key belongs on the many side:

```text
customer 1 ----- * purchase_order
customer_id PK     order_id PK
                   customer_id FK
```

Avoid storing comma-separated order IDs in `customer`. That destroys referential integrity, makes joins awkward, and turns simple updates into string surgery.

## Keys have different jobs

- A **primary key** is the table’s chosen row identity. In InnoDB it also determines clustered organization.
- A **candidate key** is any minimal set of columns that uniquely identifies a row.
- A **natural key** has business meaning, such as a country code. It can change.
- A **surrogate key** is generated for identity, often `BIGINT` or a UUID-like value.
- A **foreign key** requires a referenced parent key and protects referential integrity.
- A **composite key** uses multiple columns; column order later matters for indexes.

An SDE-2 answer avoids “always use UUID” or “always use auto-increment.” Compare insertion locality, global generation, exposure risk, storage width, secondary-index cost, and migration needs.

## Constraints make races boring

Suppose two requests both run this check:

```sql
SELECT COUNT(*) FROM customer WHERE email = 'reader@example.com';
```

Both can see zero and both can insert. The application check improves the error message but does not close the race. This does:

```sql
ALTER TABLE customer
    ADD CONSTRAINT uq_customer_email UNIQUE (email);
```

Useful constraints include `NOT NULL`, `UNIQUE`, `PRIMARY KEY`, `FOREIGN KEY`, `CHECK`, and safe defaults. A default is not a substitute for a required business decision; it is appropriate only when omission has a stable meaning.

## Normalization by dependency

Use normalization to stop one fact from being repeated in places that can disagree.

Consider:

```text
order_id | customer_id | customer_email | product_id | product_name | quantity
```

The order ID determines the customer, product ID determines product name, and an order can contain many products. Keeping everything in one table causes:

- **update anomaly:** changing a product name requires many rows;
- **insert anomaly:** a product cannot exist until an order does;
- **delete anomaly:** deleting the last order may erase the product fact.

A clearer model separates `customer`, `purchase_order`, `product`, and `order_item`. The order item can use `(order_id, product_id)` as a uniqueness constraint even if it has a surrogate row ID.

### When denormalization is deliberate

Copying `unit_price_cents` into an order item is often correct: it records the price agreed at purchase time, not a duplicate of the product’s current price. Denormalization is defensible when you state:

1. which read or historical requirement it serves;
2. which value is authoritative;
3. how the copy is updated or why it is immutable;
4. how drift is detected.

## Relationship edge cases

| Requirement | Schema decision | Common mistake |
|---|---|---|
| Delete customer but retain orders | soft-delete/status customer, or retain legal snapshot | `ON DELETE CASCADE` erases records |
| Exactly one active cart per customer | generated/explicit active key strategy plus unique constraint | check-then-insert race |
| Order lines cannot repeat product | `UNIQUE(order_id, product_id)` | enforce only in a Java `Set` |
| Money | integer minor units or intentional `DECIMAL(p,s)` | `FLOAT`/`DOUBLE` for exact currency |
| Optional relationship | nullable FK only when “unknown/absent” is valid | magic parent ID such as `0` |

## Predict the outcome

```sql
INSERT INTO customer(customer_id, email, status)
VALUES (1, 'a@example.com', 'ACTIVE');

INSERT INTO customer(customer_id, email, status)
VALUES (2, 'a@example.com', 'ACTIVE');
```

The second statement fails on `uq_customer_email`. The important result is not the vendor error text; it is that the database serialized the competing claims to one unique value.

## Debug the design

**Broken:** `order(status VARCHAR(255), customer_email VARCHAR(255), product_ids TEXT)`.

**Repair:** introduce constrained status vocabulary, reference `customer_id`, and model order items as rows. Then name the delete rule and the uniqueness rule.

## Interview angle

**Question:** “Would you keep a foreign key in a high-throughput service?”

**Strong answer:** Begin with the correctness it provides. Then discuss write cost, lock interactions, operational ownership, cross-database boundaries, and whether an event-driven repair process can truly replace the guarantee. Do not remove it because of a blanket performance claim.

## Quick check

1. How is a candidate key different from the chosen primary key?
2. Why does a Java existence check not enforce uniqueness?
3. When can a copied column be a historical fact rather than harmful duplication?
4. What must you decide before choosing `ON DELETE CASCADE`?

## Practice

- **Foundation:** Model students, courses, and enrollments with keys and constraints.
- **Interview Core:** Add idempotent payment-request creation without check-then-insert.
- **SDE-2 Follow-up:** Design a zero-downtime change from nullable to required `customer_id`.

## Solution sketch

Use an enrollment bridge with a unique `(student_id, course_id)` pair. Give payment requests a business-scoped idempotency key. For the nullability change: add/accept the column, dual-write or backfill in bounded batches, verify, add the constraint using the safest supported deployment procedure, switch reads, and remove obsolete compatibility code later.
