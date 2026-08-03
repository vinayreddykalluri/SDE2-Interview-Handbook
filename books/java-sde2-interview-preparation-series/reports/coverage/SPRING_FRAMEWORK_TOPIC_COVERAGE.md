# Spring Framework Topic Coverage

| Topic | Required depth | Previous state | Final state | Chapter | Examples/exercises | Validation |
|---|---|---|---|---|---|---|
| Framework, Boot, IoC, DI | Foundation | roadmap phrase | plain Java to container mental model | 00-01 | graph, first context, checks | companion + fixture |
| `BeanFactory`/`ApplicationContext` | Interview Core | missing | responsibilities and refresh sequence | 01 | startup debugging | fixture context |
| Bean definitions and scanning | Foundation | listed only | registration styles, names, imports, scan boundaries | 02 | explicit/scanned configs | source audit |
| Injection and resolution | Interview Core | missing | constructor, primary, qualifier, collection, provider, generic, cycles | 03 | decision tree + refactors | companion resolution checks |
| Environment, profiles, resources | Interview Core | listed only | typed settings, precedence boundary, resource/message semantics | 04 | safe configuration exercises | source audit |
| Lifecycle | Interview Core | listed only | init, destruction, aware, active lifecycle, shutdown | 05 | ordered trace | real init/destroy test |
| Extension points | SDE-2 | missing | factory and bean post-processors, `FactoryBean`, scope SPI | 05, 18 | selection matrix | source audit |
| Scopes and thread safety | Interview Core | listed only | singleton/prototype/web scopes, provider, scoped proxy, ownership | 06 | race/prototype scenarios | real prototype test |
| Configuration proxy modes | Interview Core | missing | full/lite behavior, parameters, modular imports | 07 | duplicate-construction debug | source audit |
| Application events | Interview Core | listed only | synchronous default, transaction phases, outbox/durability | 08 | event/outbox scenarios | sync + after-commit tests |
| Conversion/validation/binding | Interview Core | listed only | conversion, formatting, validators, DTOs, over-posting, SpEL safety | 09 | boundary classification | source audit |
| AOP vocabulary and design | Interview Core | listed only | pointcut/advice/aspect, order, privacy, tests | 10 | timing and audit examples | real AspectJ-style Spring AOP test |
| Proxy mechanics | SDE-2 | listed only | JDK/subclass, self-invocation, visibility, runtime type | 11 | diagnostic checklist | proxy + self-call test; companion |
| Transaction abstraction | Interview Core | listed only | manager, begin/commit/rollback, templates, boundary placement | 12 | rollback and remote-call designs | real JDBC transaction tests |
| Propagation/isolation | SDE-2 | listed only | seven modes, rollback-only, nested/new, resource capacity | 13 | traces and capacity tasks | source audit |
| Rollback/retries/idempotency | SDE-2 | missing | defaults, ordering, classification, uncertainty, outbox | 12-14 | incident and retry drills | real checked/unchecked rollback tests |
| Spring MVC | Interview Core | listed only | dispatcher flow, binding, DTO, status, advice, filter/interceptor | 15 | API scenarios | source audit |
| Async and scheduling | SDE-2 | missing | proxy, executor, queue, context, cluster coordination, durability | 16 | executor/job drills | source audit |
| Testing | SDE-2 | listed only | unit/context/proxy/transaction/MVC, caching and timeout traps | 17 | test-design exercises | Maven JUnit fixture |
| Architecture and production | SDE-2 | missing | ports/adapters, diagnostics, observability, security, extension review | 18 | incidents/playbooks | source audit |
| Real interviews | SDE-2 | missing | 24 interviewer prompts with direct model answers | 19 | realistic rounds | editorial review |
| Practice and readiness | Foundation to SDE-2 | missing | 60 prompts, 15 debugging tasks, five assessments, solutions | 20 | distributed + final practice | inventory check |

## Cross-book boundaries

- Boot auto-configuration, Actuator, packaging, and Boot test slices -> SD 05.
- Repository generation and Spring Data modules -> SD 06.
- InnoDB isolation, indexes, and locks -> SD 02.
- Entity lifecycle and ORM fetch behavior -> SD 03.
- Security, WebFlux, Batch, and Integration depth -> SD 10.

All concepts needed to compile and understand this volume's examples are included locally.
