# Mapping Identity, Values, and Schema Boundaries

An entity mapping is executable database design. The annotations should match—not invent—the table’s identity, nullability, width, uniqueness, and relationships.

## Access type

Place `@Id` on a field for field access or on a getter for property access. Mixing annotations without explicit `@Access` creates confusing behavior. With field access, Hibernate reads/writes persistent fields directly; domain methods can still protect mutation.

## Identifier strategies

Common choices include assigned IDs, database identity columns, sequences where supported, and UUIDs. Compare:

- ability to know ID before insert;
- insert batching behavior;
- database portability;
- allocation/gaps;
- key width and locality;
- cross-service generation.

Do not implement `equals` around a generated ID as though two transient entities with null IDs are equal.

## Value types and embeddables

```java
@Embeddable
public class Money {
    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
}
```

An embeddable has value semantics and no independent entity identity. Keep invariants in its constructor/methods. A Java `record` can be useful in other layers; verify provider support and persistence requirements before treating every value as an entity.

## Enums and converters

Prefer `@Enumerated(EnumType.STRING)` over ordinal storage because reordering constants changes ordinal meaning. Renaming a string constant is still a data migration.

An `AttributeConverter` maps a domain value to one column. Keep conversion deterministic and side-effect free. It does not replace a database constraint and may affect query parameter conversion.

## Inheritance is a database trade-off

- `SINGLE_TABLE`: one table, discriminator, fast polymorphic query, nullable subtype columns.
- `JOINED`: normalized subtype tables, joins for polymorphic reads.
- `TABLE_PER_CLASS`: duplicated columns and union-like polymorphic access.

Often composition or separate aggregates are clearer. Choose inheritance only for a true subtype model and inspect generated SQL.

## Identity and equality

A robust equality strategy must survive transient, managed, proxied, and detached states.

Options:

- immutable, database-unique business key used from construction;
- generated identifier equality only after assignment, with careful collection use;
- reference identity within a persistence context when value equality is unnecessary.

Never include mutable fields or associations in `hashCode` for an entity placed in a `HashSet`; changing them makes the object unreachable in its bucket. Hibernate proxies also make strict `getClass()` comparisons tricky; use a provider-aware strategy if entities can be proxied.

## Schema constraints remain authoritative

```java
@Column(nullable = false, length = 20)
private String status;
```

This helps metadata and generated schema; it is not proof the production table matches. Keep migrations, database constraints, Bean Validation, and domain validation aligned. Each fails at a different boundary.

## Mapping edge cases

| Mapping | Failure | Better reasoning |
|---|---|---|
| `double` money | approximation | exact minor units/decimal value |
| ordinal enum | reorder corrupts meaning | stable code/string plus migration plan |
| mutable natural ID in hash | broken set/map membership | immutable unique key |
| `@Lob` for ordinary text | fetch/storage surprises | choose DB type from size/access |
| entity for every table | domain graph mirrors schema accidentally | model aggregate behavior deliberately |
| eager relationships everywhere | joins/query storms | per-use-case fetch plan |

## Quick check and practice

1. How is entity identity different from value equality?
2. Why is `EnumType.STRING` safer but not migration-free?
3. What can schema annotations not prove about production?

- **Foundation:** Map an immutable `Address` embeddable.
- **Interview Core:** Choose ID generation for a high-write order table and justify it.
- **SDE-2 Follow-up:** Design equality for a proxied entity with an immutable business key.
