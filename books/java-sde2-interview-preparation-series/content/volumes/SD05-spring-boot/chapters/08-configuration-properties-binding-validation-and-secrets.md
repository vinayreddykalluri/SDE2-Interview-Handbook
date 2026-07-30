# Configuration Properties, Binding, Validation, and Secrets

`@Value` is acceptable for one isolated value. A service capability with multiple related settings deserves a typed configuration object with validation, units, defaults, and ownership.

## Immutable typed configuration

```java
@ConfigurationProperties("payment")
@Validated
public record PaymentProperties(
        @NotNull URI baseUrl,
        @NotNull @DurationMin(millis = 50) Duration connectTimeout,
        @NotNull @DurationMin(millis = 100) Duration readTimeout,
        @Min(1) @Max(20) int maxConnections) {
}
```

Register it explicitly:

```java
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PaymentProperties.class)
class PaymentConfiguration { }
```

or scan a narrow application-owned package with `@ConfigurationPropertiesScan`.

## Relaxed binding

The canonical property is kebab case:

```properties
payment.base-url=https://payments.example
payment.connect-timeout=250ms
payment.read-timeout=2s
payment.max-connections=8
```

Boot can bind common variants from environment sources. Do not depend on ambiguous acronyms or undocumented conversions. Keep names stable and generate metadata.

## Conversion and units

Boot converts durations and data sizes:

```java
@ConfigurationProperties("upload")
record UploadProperties(DataSize maxFileSize, Duration timeout) { }
```

Always include explicit units in configuration such as `10MB` and `750ms`. Bare numbers can depend on annotation/default-unit conventions and are easy to misread.

## Fail fast on unsafe configuration

Validation should reject impossible state during startup:

- blank endpoint;
- negative timeout;
- zero pool size;
- unsupported region;
- retry count outside a tested bound.

Cross-field rules need a compact constructor or class-level validator:

```java
public PaymentProperties {
    if (readTimeout.compareTo(connectTimeout) < 0) {
        throw new IllegalArgumentException(
                "payment.read-timeout must not be shorter than connect-timeout");
    }
}
```

Do not validate remote reachability in a constructor. That turns transient network failure into context creation failure and may leak credentials.

## Secrets are references and values with stricter handling

Configuration binding does not make a value safe. Secrets require:

- delivery from an approved store or mounted secret;
- least-privilege access;
- rotation strategy;
- redaction from logs, heap dumps, endpoints, and error messages;
- no default fallback that silently uses an unsafe credential.

Prefer a credential-provider abstraction when values rotate during process lifetime. An immutable bound string cannot refresh itself.

## `@Value` versus properties

| `@Value` | `@ConfigurationProperties` |
|---|---|
| one local scalar | cohesive capability settings |
| string expression support | structured binding and metadata |
| easy to scatter | central ownership and validation |
| weaker refactoring | typed nested models |

## Test binding directly

```java
new ApplicationContextRunner()
    .withUserConfiguration(PaymentConfiguration.class)
    .withPropertyValues(
        "payment.base-url=https://payments.example",
        "payment.connect-timeout=250ms",
        "payment.read-timeout=2s",
        "payment.max-connections=8")
    .run(context -> assertThat(context)
        .hasSingleBean(PaymentProperties.class));
```

Add invalid and missing-value cases. A happy-path context test alone does not prove the safety boundary.

## Common mistakes

- Scattering related values across fields with `@Value`.
- Using strings for durations and parsing them repeatedly.
- Providing unsafe defaults for credentials or production endpoints.
- Expecting bean validation without the validation dependency/infrastructure.
- Logging an entire properties object containing secrets.
- Assuming a mounted secret rotates inside an already bound object.

## Interview angle

**Interviewer:** Why did a typo in a property start the service with a default?

**Strong answer:** The property did not bind to the canonical field, and the target allowed a default. I add metadata, fail-fast validation for required values, a binding test using the deployment name, and an ownership rule that rejects unknown or obsolete properties where practical. I do not solve it by printing all configuration.

## Quick check

1. When is `@Value` sufficient?
2. Why include units in configuration?
3. What belongs in startup validation?
4. Why should a constructor avoid remote reachability checks?
5. What changes when a secret rotates?

## Practice

- **Foundation:** Convert five `@Value` fields into one record.
- **Interview Core:** Add valid, missing, and cross-field-invalid binding tests.
- **Interview Core:** Design redaction for a properties object.
- **SDE-2 Follow-up:** Support credential rotation without restarting every instance simultaneously.
