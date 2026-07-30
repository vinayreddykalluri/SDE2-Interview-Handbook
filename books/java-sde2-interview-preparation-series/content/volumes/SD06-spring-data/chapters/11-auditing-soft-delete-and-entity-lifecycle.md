# Auditing, Soft Delete, and Entity Lifecycle

Repository code often appears in interview problems that need immutable history, compliance fields, and reversible deletes.

## Core concepts

### Auditing

Audit fields (`createdBy`, `createdAt`, `updatedBy`, `updatedAt`) are operational evidence.

### Soft delete

Soft delete keeps row history and allows replaying compliance checks.

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

## Quick check

1. Why is soft delete often required in interview scenarios?
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
