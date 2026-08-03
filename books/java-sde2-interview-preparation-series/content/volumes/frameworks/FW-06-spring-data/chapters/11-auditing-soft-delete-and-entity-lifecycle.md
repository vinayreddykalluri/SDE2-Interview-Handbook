# Auditing, Soft Delete, and Entity Lifecycle

Repository code often appears in interview problems that need immutable history, compliance fields, and reversible deletes.

## Core concepts

### Auditing

Audit fields (`createdBy`, `createdAt`, `updatedBy`, `updatedAt`) are operational evidence.

Spring Data auditing can populate these fields through `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`, and an `AuditorAware` provider. That is convenient metadata population, not a tamper-proof audit log. Bulk SQL, another application, or a broken principal provider can bypass or misattribute it.

### Soft delete

Soft delete keeps row history and allows replaying compliance checks.

Soft delete is a product/legal choice, not a universal requirement. It does not by itself satisfy retention, restore, privacy erasure, or immutable audit requirements.

### Lifecycle visibility

Repository methods should expose lifecycle state explicitly:

- created/active,
- archived,
- deleted/expired,
- restored.

## Interview pattern

Use dedicated repository methods for lifecycle states and avoid reusing broad `findAll` over mixed states.

```text
read model
  -> active-only methods
  -> explicit archive/read-only methods
  -> explicit restore/reject methods
```

## Common issue

Soft-delete flags without index support can cause full scans.
Not including tenant/user separation in indexes can create accidental cross-tenant exposure.

Uniqueness also needs an explicit policy: may a deleted username or external ID be reused? The answer may require a partial/generated index, a lifecycle column in the key, anonymization, or permanent reservation depending on the target database and product rule.

## Edge matrix

| Edge | Required decision |
|---|---|
| bulk update bypasses callbacks | database/application audit alternative and tests |
| background task has no user principal | explicit system actor |
| clock differs across nodes | authoritative instant/source |
| deleted row referenced by child | restore and referential policy |
| global filters hide rows | administrative/reconciliation path with authorization |

## Quick check

1. When is soft delete justified, and when is it not enough?
2. What is the risk of no lifecycle-specific methods?
3. Why are auditable fields also a debugging tool?

## Debugging exercise

A bulk report includes deleted rows and active rows because filter condition is missing.

How do you fix query layer and index layer?

Expected: lifecycle-aware repository method, explicit compound index on status+tenant, and regression tests.

## Practice

- **Foundation:** Add three lifecycle states to one domain entity.
- **Interview Core:** Explain soft delete with recovery and evidence requirements.
- **SDE-2 Follow-up:** Describe how you prevent accidental exposure of soft-deleted rows.

## Interviewer question and model answer

**Interviewer:** Does `@CreatedDate` give me a compliance audit log?

**Model answer:** It gives convenient entity metadata when Spring Data lifecycle hooks run. It is not automatically immutable, complete, independently retained, or tamper-evident, and bulk/native writes can bypass it. I separate operational timestamps from a required audit/event record, define the actor and clock source, protect retention and access, and test every write path that claims to produce audit evidence.
