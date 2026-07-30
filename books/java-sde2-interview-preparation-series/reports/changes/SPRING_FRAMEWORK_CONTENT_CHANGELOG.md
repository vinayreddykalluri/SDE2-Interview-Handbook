# Spring Framework Content Changelog

| Chapter(s) | Original weakness | Change made | Accuracy correction / evidence |
|---|---|---|---|
| 00-01 | no beginner entry or runnable context | added plain Java graph, first context, refresh trace | Framework separated from Boot; bean separated from object |
| 02-04 | registration/configuration only named | added definitions, scanning, imports, candidate resolution, profiles, resources | annotation is metadata; Framework precedence not conflated with Boot |
| 05-07 | lifecycle, scope, configuration mechanics absent | added lifecycle pipeline, processors, scopes, providers, full/lite config | singleton is per bean/definition/container; prototype destruction limit; inter-bean interception labeled |
| 08-09 | events/validation listed without semantics | added sync/transaction events, outbox, conversion, validation, binding security | events not automatically async/durable; validation not a concurrency guarantee |
| 10-11 | AOP/proxy behavior missing | added vocabulary, pointcuts, order, JDK/subclass proxies, self-invocation | final/private/self calls cannot be assumed advised |
| 12-14 | transactions named but not taught | added manager/resource model, rollback rules, propagation, isolation, retries, uncertainty, outbox | checked rollback default, rollback-only, `REQUIRES_NEW` resource cost, fresh-attempt retry |
| 15 | MVC was only a future bullet | added dispatcher pipeline, DTOs, status/errors, filter/interceptor/advice | Framework MVC kept distinct from Boot configuration |
| 16 | async/scheduling absent | added executor, context/thread, failures, cluster job ownership | async not durable; transactions/request scope do not automatically propagate |
| 17 | testing only named | added test ladder, transaction commit traps, context caching | rollback test does not prove commit; preemptive timeout thread risk |
| 18 | no extension/production diagnosis | added architecture, extension selection, startup/proxy/performance/security playbooks | evidence before proxy/pool/config changes |
| 19-20 | no interview or practice material | added 24 answered rounds, 60 prompts, 15 debug tasks, solutions, five assessments | direct model answers rather than question titles |

## Executable additions

- `SpringFrameworkInterviewCompanion.java`: dependency order/cycle, candidate resolution, proxy crossing, and rollback policy.
- Maven Spring fixture: actual container injection/scopes/lifecycle, events, AOP, self-invocation, and JDBC transaction rollback behavior.
- `validate_spring_framework_labs.sh`: warning-free Java 21 compilation, assertion execution, and Maven test gate.

## Publishing changes

The existing Markdown-to-PDF and web synchronization pipeline was preserved. Only SD 04 source registration, validation hooks, generated catalog data, and its stable PDF artifact were updated.
