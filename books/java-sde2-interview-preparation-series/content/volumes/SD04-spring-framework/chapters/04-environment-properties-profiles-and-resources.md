# Environment, Properties, Profiles, and Resources

Configuration is input to the object graph. Treat it as typed, validated data with explicit ownership, not as strings fetched throughout business code.

## `Environment` and property sources

The Spring `Environment` represents profiles and property resolution. Property sources can include JVM system properties, environment variables, and explicitly registered files or maps. Precedence matters: the first matching property source wins according to the configured order.

```java
@Bean
PaymentClient paymentClient(Environment environment) {
    String baseUrl = environment.getRequiredProperty("payments.base-url");
    Duration timeout = environment.getRequiredProperty(
            "payments.timeout", Duration.class);
    return new PaymentClient(baseUrl, timeout);
}
```

This is reasonable at a configuration boundary. Passing `Environment` into domain services lets arbitrary keys spread through the codebase and makes required input invisible.

## Property-token injection

```java
final class PaymentSettings {
    private final Duration timeout;

    PaymentSettings(@Value("${payments.timeout:2s}") Duration timeout) {
        this.timeout = timeout;
    }
}
```

`@Value` can resolve property tokens and expressions when the appropriate infrastructure is registered. It is convenient for a few values. For a larger related set, build a typed settings object in configuration, validate it once, and inject that object.

## Precedence must be tested

```text
high priority  test override
               command/JVM property
               environment-derived source
               application file
low priority   code default
```

This diagram is illustrative, not a universal promise. Spring Boot defines extensive external-configuration precedence; plain Framework applications define the property sources they add. State which environment you mean.

## Profiles select graphs, not business branches

```java
@Configuration
@Profile("local")
class LocalPaymentConfiguration {
    @Bean
    PaymentGateway paymentGateway() {
        return new StubPaymentGateway();
    }
}
```

Profiles are useful for coarse environment or deployment variants. Avoid a combinatorial set such as `local`, `cloud`, `eu`, `async`, `debug`, `new-checkout`. Prefer explicit configuration objects and feature-decision services for independent runtime choices.

Profile expressions can combine conditions, but complexity is still complexity. A graph that is impossible to visualize is difficult to validate before deployment.

## `Resource` abstracts location

```java
@Bean
TermsService termsService(ApplicationContext context) {
    Resource resource = context.getResource("classpath:terms/default.txt");
    return new TermsService(resource);
}
```

Common prefixes include `classpath:` and `file:`. A `Resource` may not be a real filesystem `File`; classpath resources inside a JAR are a classic example. Use `getInputStream()` when streaming is the actual requirement.

```java
try (InputStream input = resource.getInputStream()) {
    // read with an explicit charset and bounded size
}
```

Never assume a resource is repeatable or small. Close streams and apply size limits to untrusted content.

## Message resolution

`ApplicationContext` also implements `MessageSource`, supporting codes, arguments, and locales:

```java
String message = context.getMessage(
        "order.not-found", new Object[]{orderId}, Locale.US);
```

Business errors should carry stable codes and structured context. Human-language messages can be resolved at the presentation boundary.

## Secrets and operational safety

- Do not commit secrets to property files.
- Keep secret values out of exception messages and configuration dumps.
- Validate presence and format during startup, but avoid contacting every remote service in bean constructors.
- Document which settings are reloadable; plain bean construction usually captures a startup snapshot.
- Record non-secret effective configuration and active profiles for diagnosis.

## Strong configuration boundary

```java
record PaymentClientSettings(URI baseUri, Duration timeout) {
    PaymentClientSettings {
        Objects.requireNonNull(baseUri);
        Objects.requireNonNull(timeout);
        if (!baseUri.getScheme().equals("https")) {
            throw new IllegalArgumentException("payments URL must use HTTPS");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }
}
```

Construct this once from external strings. Downstream code receives typed, already-validated values.

## Common mistakes

- Assuming Spring Framework and Spring Boot have identical property precedence.
- Reading configuration independently in many beans.
- Treating profile names as runtime business states.
- Calling `resource.getFile()` for a resource packaged in a JAR.
- Providing insecure production defaults for required secrets or endpoints.
- Logging tokens while diagnosing a missing property.

## Interview angle

**Interviewer:** How would you manage environment-specific clients without scattering conditionals?

**Strong answer:** I keep business services dependent on a stable interface. At configuration startup, I resolve and validate typed settings, then choose and construct one adapter through explicit configuration or a coarse profile. I test property precedence and graph selection, fail fast for invalid required values, and never expose secrets in diagnostics.

## Quick check

1. What two concerns does `Environment` model?
2. Why should property precedence be an explicit test?
3. When do profiles become difficult to manage?
4. Why may `Resource#getFile()` fail?
5. Where should message localization occur?

## Predict and debug

**Predict:** A `classpath:` resource inside an executable JAR can provide an input stream but may not have a usable filesystem path.

**Debug:** `payments.timeout=abc` reaches a request and fails late. Convert and validate it when building `PaymentClientSettings`, causing context startup to fail with the property name and safe reason.

## Practice

- **Foundation:** Load one required string and one `Duration` from `Environment`.
- **Foundation:** Read a classpath text resource without converting it to `File`.
- **Interview Core:** Design a typed settings record for a database client.
- **Interview Core:** Test an override property against a default property source.
- **SDE-2 Follow-up:** Replace eight interacting profiles with two deployment profiles and explicit feature configuration.

## Readiness checkpoint

Continue when you can turn external strings into a small validated configuration object and explain resource and profile boundaries without borrowing unmentioned Boot behavior.
