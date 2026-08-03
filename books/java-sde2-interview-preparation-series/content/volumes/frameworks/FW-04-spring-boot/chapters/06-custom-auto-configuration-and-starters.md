# Custom Auto-Configuration and Starters

Application teams should use explicit configuration first. Custom auto-configuration is appropriate for a reusable library that must integrate with many Boot applications while allowing each application to replace defaults.

## Separate library responsibilities

```text
acme-audit-core
  domain API and implementation

acme-audit-spring-boot
  configuration properties and auto-configuration

acme-audit-spring-boot-starter
  dependency descriptor for consumers
```

Small teams may combine the last two, but the conceptual separation remains: runtime implementation, conditional assembly, dependency convenience.

## A safe auto-configuration

```java
@AutoConfiguration
@ConditionalOnClass(AuditSink.class)
@ConditionalOnProperty(
        prefix = "acme.audit",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(AuditProperties.class)
public class AuditAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    AuditSink auditSink(AuditProperties properties) {
        return new HttpAuditSink(properties.endpoint(), properties.timeout());
    }
}
```

Register it in:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

with the fully qualified class name. Do not rely on application component scanning to discover library auto-configuration.

## Ordering without hidden coupling

Use `@AutoConfigureBefore`, `@AutoConfigureAfter`, or the corresponding names only when one auto-configuration consumes definitions from another. Ordering affects definition processing; it does not force bean creation order. Bean dependencies still determine creation.

Avoid depending on internal Boot auto-configuration implementation classes. Their public behavior and bean types are safer integration points.

## Metadata and IDE support

The configuration processor produces metadata for typed properties. Document:

- property name and purpose;
- type and unit;
- default behavior;
- whether restart is required;
- security sensitivity;
- validation rules and deprecation path.

Do not use generated metadata as runtime validation. Binding and bean validation own correctness.

## Test the decision matrix

```java
private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class));

@Test
void backsOffForUserSink() {
    runner.withBean(AuditSink.class, InMemoryAuditSink::new)
          .run(context -> assertThat(context)
                  .hasSingleBean(AuditSink.class)
                  .getBean(AuditSink.class)
                  .isInstanceOf(InMemoryAuditSink.class));
}
```

Required cases include default match, property disabled, required class absent, required bean present/absent, user override, invalid properties, and relevant application types.

## Compatibility contract

A starter becomes shared platform code. It needs:

- a supported Boot version range;
- dependency convergence tests;
- release notes and migration guidance;
- observability and failure semantics;
- no silent remote calls during startup unless explicitly contracted;
- a rollback plan if adoption breaks services.

## Common mistakes

- Scanning library packages instead of using imports metadata.
- Making auto-configuration classes an application API.
- Creating a bean even when the user provided one.
- Fetching remote configuration in a bean constructor.
- Making a starter depend on unrelated databases, web stacks, or logging systems.
- Publishing without testing the packaged jar in a consuming application.

## Interview angle

**Interviewer:** When would you build a company starter?

**Strong answer:** When multiple services need the same stable integration contract and defaults, not merely the same three annotations. I separate the core API from conditional Boot assembly, back off for user beans, expose validated typed configuration, test the condition matrix with `ApplicationContextRunner`, and version the starter like a platform product.

## Quick check

1. Why not discover library configuration by component scan?
2. What belongs in a starter artifact?
3. What does auto-configuration ordering control?
4. Which cases belong in the condition test matrix?
5. Why is a company starter a long-term compatibility contract?

## Practice

- **Foundation:** Write imports metadata for one auto-configuration.
- **Interview Core:** Add a user override and property-disable test.
- **Interview Core:** Design configuration metadata for timeout and endpoint values.
- **SDE-2 Follow-up:** Review a starter that performs a remote call during context refresh.
