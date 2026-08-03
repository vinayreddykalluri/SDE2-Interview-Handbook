# MongoDB Content Changelog — Backend Wave 3

| Area | Change |
|---|---|
| sequence | replaced the roadmap with nine prerequisite-ordered chapters |
| basics | added BSON types, missing/null, CRUD, projection, update operators, validation |
| modeling | added embed/reference decision rules, snapshots, growth, hot documents, polymorphism |
| internals | added driver-to-query-plan-to-WiredTiger/cache/storage response path |
| performance | added compound/multikey/specialized indexes, execution evidence, keyset pagination |
| aggregation | added result grain, `$unwind`, `$lookup`, windows, memory/spill/materialization |
| consistency | added replication flow, concerns/preferences, causal sessions, retries/unknown commit |
| scaling | added shard-key analysis, targeting, migration, change-stream checkpoint/rebuild |
| Java/Spring | added native driver → `MongoTemplate` → repository boundary and failure contracts |
| interviews | added 7 live interviewer chains, 16 rapid answers, assessment and worked solution rubric |

## Executable additions

- `code/MongoDbInterviewCompanion.java`: embed/reference decision, versioned transitions, cursor ordering, transaction-error labels, idempotent projection.
- `labs/maven-demo`: seven official-driver tests for BSON types and filter/update/index/aggregation/cursor/transaction command shapes.
- `labs/validate_mongodb_labs.sh`: strict Java 21 compile/smoke plus Maven tests.
