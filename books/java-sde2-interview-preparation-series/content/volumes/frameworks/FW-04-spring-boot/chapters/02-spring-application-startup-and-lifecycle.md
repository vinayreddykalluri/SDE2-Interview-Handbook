# SpringApplication Startup and Lifecycle

Production diagnosis starts with a timeline. A Boot process can fail before logging is configured, while the environment is prepared, during bean creation, while the web server binds, in a startup runner, or after readiness begins.

## Simplified startup sequence

```text
main
  -> create SpringApplication
  -> discover listeners and initializers
  -> prepare Environment and bind spring.main.*
  -> print banner and create ApplicationContext
  -> load configuration sources
  -> refresh context
       -> register definitions
       -> apply auto-configuration
       -> create non-lazy singletons
       -> start embedded server
  -> publish started event
  -> run ApplicationRunner / CommandLineRunner
  -> publish ready event
```

The exact internal calls are more detailed, but this sequence is sufficient to classify failures and avoid treating every startup error as a bean error.

## Application type

Boot infers whether the application is non-web, servlet, or reactive from the classpath. Mixed web stacks make this decision harder to reason about. When a service is intentionally non-web:

```java
SpringApplication application = new SpringApplication(BatchApplication.class);
application.setWebApplicationType(WebApplicationType.NONE);
application.run(args);
```

Use an explicit type only when it communicates real intent; do not use it to hide accidental dependencies.

## Arguments and runners

```java
@Component
final class CacheWarmup implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments arguments) {
        boolean verifyOnly = arguments.containsOption("verify-only");
        // bounded startup work
    }
}
```

`ApplicationArguments` separates option and non-option arguments. `CommandLineRunner` receives raw strings. Multiple runners can implement `Ordered`, but complex ordering is a smell. A runner executes before the ready event; slow or unbounded work delays readiness and may exceed platform startup limits.

## Failure phases

| Symptom | Likely phase | First evidence |
|---|---|---|
| property binding error | environment/context preparation | origin and binding failure |
| `NoSuchBeanDefinitionException` | context refresh | dependency graph and condition report |
| port already in use | server startup | bind exception and configured port |
| process starts then exits | runner or non-web lifecycle | exit code and final exception |
| ready then fails traffic | runtime dependency or request path | health, metrics, traces, logs |

## Startup events are signals, not business messages

Boot publishes lifecycle events such as starting, environment prepared, context initialized, started, ready, and failed. Some occur before the application context exists, so ordinary bean listeners cannot receive all of them. Register early listeners on `SpringApplication` or through supported discovery when truly needed.

Do not perform critical durable work only because a ready event fired. An event is in-process; the process can terminate afterward.

## Exit codes

Command-line applications can map failures to process exit codes with `ExitCodeGenerator` or exception mappings. Operations systems depend on non-zero failure codes.

```java
int code = SpringApplication.exit(context, () -> 12);
System.exit(code);
```

Avoid `System.exit` inside ordinary services and tests. Let the application boundary own process termination.

## Measuring startup

Use `ApplicationStartup` for structured startup steps:

```java
SpringApplication application = new SpringApplication(OrderApplication.class);
application.setApplicationStartup(new BufferingApplicationStartup(2048));
application.run(args);
```

With Actuator, the `startup` endpoint can expose buffered steps. This is diagnostic data; secure its exposure and choose buffer size deliberately.

## Common mistakes

- Blocking forever inside a runner.
- Starting unmanaged threads that prevent shutdown.
- Assuming ready means every remote dependency is permanently healthy.
- Logging secrets while diagnosing environment preparation.
- Retrying a deterministic configuration error.
- adding lazy initialization globally to hide a broken bean graph.

## Interview angle

**Interviewer:** The pod is killed before becoming ready. What do you inspect?

**Strong answer:** I locate the last completed startup phase from events and logs, separate environment binding, context refresh, server binding, and runner work, inspect the condition report for missing infrastructure, compare startup duration with probe thresholds, and reproduce the packaged artifact with the same effective configuration. I do not increase the probe timeout before determining whether work is bounded.

## Quick check

1. When do runners execute relative to readiness?
2. Why can an early lifecycle listener not be an ordinary bean?
3. What decides application type?
4. How should a CLI failure reach its supervisor?
5. What does `ApplicationStartup` provide?

## Practice

- **Foundation:** Add a runner that prints parsed option names.
- **Interview Core:** Classify five startup stack traces by phase.
- **Interview Core:** Make a startup task bounded and observable.
- **SDE-2 Follow-up:** Design startup for a cache that improves latency but is not required for correctness.
