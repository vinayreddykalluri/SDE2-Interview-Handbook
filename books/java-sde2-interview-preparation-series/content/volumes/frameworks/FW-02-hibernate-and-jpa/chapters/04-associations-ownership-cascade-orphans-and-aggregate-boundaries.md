# Associations, Ownership, Cascades, Orphans, and Aggregate Boundaries

Associations are where convenient object navigation most often creates incorrect SQL and uncontrolled graphs.

## Foreign-key ownership

For an order with lines, the foreign key is on `order_line`. The child side usually owns the relationship:

```java
@Entity
class OrderLine {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;
}

@Entity
class PurchaseOrder {
    @OneToMany(mappedBy = "order",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    public void addLine(OrderLine line) {
        lines.add(line);
        line.attachTo(this);
    }

    public void removeLine(OrderLine line) {
        lines.remove(line);
        line.detachFrom(this);
    }
}
```

`mappedBy` says the collection is inverse; changing only it may leave the owning foreign-key side unchanged. Helper methods keep both in-memory sides consistent.

## Cascade is an entity-operation policy

Cascade tells JPA whether operations such as persist, merge, remove, refresh, or detach propagate across an association. It is not the same as database `ON DELETE CASCADE`, which the database enforces even for direct SQL.

Use cascade from an aggregate root to privately owned children. Avoid `CascadeType.REMOVE` across shared many-to-many relationships: deleting one user must not delete shared roles.

## Orphan removal

With `orphanRemoval = true`, removing a child from the owned association schedules deletion of that child row. This fits order lines that have no meaning outside the order. It is not a generic “clean unused rows” feature.

Expected trace:

```sql
select ... from purchase_order where order_id=?;
select ... from order_line where order_id=?;
delete from order_line where line_id=?;
```

The delete is normally emitted at flush, not at `List.remove` itself.

## Aggregate boundaries

An aggregate is a consistency boundary, not every object reachable by a getter. Keep transactional invariants local. References to another aggregate often need only its identifier or a lazy many-to-one, not cascading lifecycle ownership.

Large bidirectional graphs increase:

- accidental loads and serialization recursion;
- cascade traversal and dirty-check work;
- unclear delete responsibility;
- equality/debugging complexity.

Prefer unidirectional mappings unless both directions serve real domain operations.

## Many-to-many warning

A direct `@ManyToMany` hides the join table as soon as the relationship needs attributes such as role, created time, ordering, or status. Model an explicit association entity (`Membership`) with its own identity/invariants. It also gives precise update/delete control.

## Collection choices

- `List` permits order/duplicates; persistent ordering needs `@OrderColumn` or an ordered query/mapping.
- `Set` depends on stable `equals`/`hashCode`.
- `Map` requires deliberate persistent key semantics.

Do not choose `Set` as a performance hack. Database uniqueness still needs a constraint.

## Failure matrix

| Symptom | Cause | Repair |
|---|---|---|
| child FK remains null | only inverse side changed | synchronize owning side |
| deleting group deletes users | remove cascade across shared relation | model ownership correctly |
| removed child row remains | no orphan removal/direct FK update | define lifecycle and inspect flush SQL |
| stack overflow in JSON/logging | bidirectional traversal | DTO boundary and controlled `toString` |
| duplicate children despite `Set` | unstable equality/no DB constraint | stable equality plus unique constraint |

## Practice

- **Foundation:** Identify the owner for a foreign key on `order_line`.
- **Interview Core:** Choose cascade and orphan behavior for order lines versus customer.
- **SDE-2 Follow-up:** Replace a many-to-many user-role mapping when membership gains expiration and audit fields.
