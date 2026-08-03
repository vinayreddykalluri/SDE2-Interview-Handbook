# MySQL Types, NULL, Collation, Temporal Values, and Schema Safety

Types encode a domain and determine comparison, storage, conversion, index width, and client behavior. “Use `VARCHAR` for everything” throws away those guarantees.

## Numeric choices

| Need | Candidate | Reasoning |
|---|---|---|
| row count or identifier | sized integer, often `BIGINT` | exact arithmetic and predictable range |
| money | integer minor units or `DECIMAL(p,s)` | exact decimal value |
| scientific approximation | `DOUBLE` | range and approximate arithmetic |
| boolean flag | `BOOLEAN` alias/`TINYINT` with domain convention | document allowed values |

`DECIMAL(12,2)` supports ten integer digits and two fractional digits. It does not automatically establish currency, rounding mode, or whether negative values are legal—those remain domain rules.

## Text, bytes, character sets, and collations

`VARCHAR` stores text using a character set. `VARBINARY` stores bytes. A collation decides comparison and ordering behavior, including case and accent sensitivity. Therefore a unique index on an email or username can behave differently under different collations.

Do not silently lowercase values as a universal fix. Decide whether the identifier is case-sensitive, how Unicode normalization is handled, and whether display spelling is distinct from comparison spelling.

```sql
CREATE TABLE tag (
    tag_id BIGINT PRIMARY KEY,
    name VARCHAR(100) CHARACTER SET utf8mb4
        COLLATE utf8mb4_0900_ai_ci NOT NULL,
    CONSTRAINT uq_tag_name UNIQUE (name)
);
```

This example is MySQL 8-specific. The collation is accent- and case-insensitive; use a binary or other deliberate collation when that is not the product rule.

## `NULL` means unknown or absent—not zero or empty

SQL uses three-valued logic: a predicate can be `TRUE`, `FALSE`, or `UNKNOWN`.

```sql
SELECT * FROM customer WHERE status = NULL;     -- never the intended test
SELECT * FROM customer WHERE status IS NULL;    -- correct null test
```

`NULL = NULL` is `UNKNOWN`, not `TRUE`. In a `WHERE` clause, only `TRUE` rows survive.

### A small truth table

| Expression | Result |
|---|---|
| `1 = 1` | `TRUE` |
| `1 = NULL` | `UNKNOWN` |
| `NULL = NULL` | `UNKNOWN` |
| `NULL IS NULL` | `TRUE` |
| `TRUE AND UNKNOWN` | `UNKNOWN` |
| `FALSE AND UNKNOWN` | `FALSE` |

`NOT IN` is a famous trap when the subquery can contain `NULL`:

```sql
-- Fragile when banned_customer.customer_id can be NULL
WHERE customer_id NOT IN (SELECT customer_id FROM banned_customer)

-- State the anti-join explicitly
WHERE NOT EXISTS (
    SELECT 1
    FROM banned_customer b
    WHERE b.customer_id = c.customer_id
)
```

## Time is a data-model decision

Store an instant when the requirement is “a moment on the global timeline.” Store a local date/time plus zone/recurrence rules when the requirement is “9:00 every day in Atlanta.” A UTC timestamp alone cannot preserve a future civil schedule when zone rules change.

MySQL `TIMESTAMP` and `DATETIME` differ in range and timezone conversion behavior. Verify the selected MySQL version and JDBC driver behavior; do not rely on the server/session default timezone. In Java, prefer `Instant` for instants, `LocalDate` for dates, and `ZonedDateTime`/zone identifiers for civil schedules.

## JSON is not a schema escape hatch

MySQL JSON is valuable for genuinely variable attributes, but frequently filtered or constrained fields deserve explicit modeling or generated columns with indexes. Ask:

- Can the database validate the shape?
- Which paths appear in predicates?
- How will the field evolve?
- Does partial update introduce lost-update risk?

## Conversion and strictness failures

Implicit conversions can change results and index use. Compare compatible types in predicates. Run production with deliberate SQL modes and test migrations under the same modes. A development database that truncates invalid values while production rejects them creates false confidence.

## Edge-case matrix

| Case | Risk | Safer decision |
|---|---|---|
| `VARCHAR` number compared to integer | conversion and surprising matches | store and bind the correct numeric type |
| nullable unique value | multiple `NULL` rows may be permitted | model “one absent/active” rule explicitly |
| `DOUBLE` price | equality and rounding surprises | exact representation |
| default timezone differs | shifted instants | explicit UTC/session/driver contract |
| case-insensitive collation | unexpected duplicate rejection | match collation to product identity rule |
| oversized indexed text | larger B+tree and fewer entries per page | size from domain and query evidence |

## Quick check and practice

1. Why does `column = NULL` not find nulls?
2. Why can `NOT IN` with a nullable subquery return no rows?
3. What product decision precedes choosing a collation?
4. Distinguish an instant from a recurring local schedule.

- **Foundation:** Rewrite five magic-value columns as nullable or constrained domains.
- **Interview Core:** Choose types for amount, currency, placed instant, and delivery-local window.
- **SDE-2 Follow-up:** Plan a collation change for a unique username without accepting or losing duplicates.
