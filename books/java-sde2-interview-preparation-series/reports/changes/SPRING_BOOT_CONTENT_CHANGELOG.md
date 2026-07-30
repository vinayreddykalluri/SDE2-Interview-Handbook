# Spring Boot Content Changelog

| Chapter | Original weakness | Change made | Examples/practice | Accuracy correction/cross-reference |
|---|---|---|---|---|
| 00 | Boot named only | added conditional assembly mental model and path | diagram, checks, tasks | Boot does not replace Framework/platform |
| 01 | no runnable app | added Maven Boot 4.1 MVC app and jar flow | application, endpoint, commands | Boot 4 focused webmvc starter labeled |
| 02 | no startup mechanics | added phase/event/runner/exit/startup model | timeline and failure table | ready occurs after bounded startup work |
| 03 | no structure | added package, scanning, module design | layouts and diagnosis | annotation does not guarantee registration |
| 04 | starter/build conflated | separated starter, BOM, plugin, graph | Maven/Gradle and CLI | managed version is not direct dependency |
| 05 | auto-config treated as magic | added conditions, back-off, report, exclusions | decision tree and debug | back-off is condition-specific |
| 06 | custom starter absent | added reusable auto-config lifecycle | imports metadata and test | application scanning is not library registration |
| 07 | configuration name only | added precedence/origin/profiles/imports | YAML/properties/runbook | packaged YAML is not highest universally |
| 08 | typed config absent | added immutable binding, validation, units, secrets | record and tests | binding does not make secrets safe/refreshable |
| 09 | REST too shallow | added DTO, validation layers, problem details | controller/advice/tasks | validation is not authorization |
| 10 | API behavior absent | added HTTP semantics, pagination, versioning, idempotency | state machine/tasks | timeout/retry duplicates handled durably |
| 11 | clients absent | added client builders, timeout taxonomy, retries | RestClient and recovery | timeout means unknown remote outcome |
| 12 | data listed only | added pool/transaction/migration safety | config/expand-contract | H2 does not prove MySQL behavior |
| 13 | Actuator listed only | added availability/access/exposure/security model | YAML/health/endpoint | endpoint presence is not remote exposure |
| 14 | observability absent | added metrics, observation, traces, logs | counter/observation/tasks | cardinality/privacy constraints explicit |
| 15 | probes absent | added lifecycle and graceful drain | timeline/budgets | shared dependency excluded from liveness |
| 16 | testing listed only | added evidence ladder and cache/commit traps | unit/runner/slice/full | full context is not universal default |
| 17 | packaging name only | added jar/layers/buildpack/Docker/resources | commands/Dockerfile | heap is not container memory total |
| 18 | upgrades name only | added startup measurement, AOT/native, upgrades | matrices/tasks | AOT and native are distinct |
| 19 | security absent | added Boot-level secure boundary | filter chain/threat tasks | CORS is not authentication |
| 20 | incidents absent | added seven diagnostic playbooks | evidence loop/tasks | collect evidence before restart |
| 21 | interview questions absent | added 28 realistic rounds | direct model answers | annotations replaced by mechanism/evidence |
| 22 | exercises/solutions absent | added assessments and solution sketches | 85+ final tasks | cross-book boundaries documented |

## Executable assets added

- `SpringBootInterviewCompanion.java`: dependency-free Java 21 model for five high-risk reasoning areas.
- Maven Boot 4.1 fixture: six tests for precedence, availability, conditional registration, back-off, validation, and condition reports.
- `validate_spring_boot_labs.sh`: one command with warning-free Java compilation and Maven execution.
