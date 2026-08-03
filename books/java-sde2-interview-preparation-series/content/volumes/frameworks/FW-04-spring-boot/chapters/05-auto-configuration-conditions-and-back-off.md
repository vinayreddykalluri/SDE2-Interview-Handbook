# Auto-Configuration, Conditions, and Back-Off

Auto-configuration is ordinary Spring configuration selected by evidence. Boot imports candidate classes, evaluates their conditions, and registers beans only when the application has not already provided the relevant capability.

## A representative auto-configuration

```java
@AutoConfiguration
@ConditionalOnClass(PaymentClient.class)
@EnableConfigurationProperties(PaymentProperties.class)
class PaymentClientAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    PaymentClient paymentClient(PaymentProperties properties,
                                RestClient.Builder builder) {
        return new PaymentClient(
                builder.baseUrl(properties.baseUrl().toString()).build());
    }
}
```

Read it as a decision:

```text
PaymentClient class present?
        |
        +-- no -> skip configuration
        |
        +-- yes -> PaymentClient bean already present?
                       |
                       +-- yes -> back off
                       +-- no  -> bind properties and create default
```

## Common condition types

| Condition | Question |
|---|---|
| `@ConditionalOnClass` | Is a library capability present? |
| `@ConditionalOnMissingClass` | Is a library absent? |
| `@ConditionalOnBean` | Did the application register a prerequisite? |
| `@ConditionalOnMissingBean` | Has the application already supplied a replacement? |
| `@ConditionalOnProperty` | Is a feature property present or equal to a value? |
| `@ConditionalOnWebApplication` | Is this servlet/reactive web context? |
| `@ConditionalOnResource` | Does a resource exist? |

Conditions are evaluated during registration. Bean conditions see definitions processed so far, which is why auto-configuration is applied after user configuration. Do not make ordering accidental.

## Back-off is local

Providing one `ObjectMapper`, `DataSource`, or `RestClient.Builder` may cause some defaults to back off while related auto-configurations remain. Replacing one bean does not disable an entire subsystem unless its conditions say so.

```java
@Bean
PaymentClient paymentClient(RestClient.Builder builder) {
    return new PaymentClient(builder.baseUrl("https://sandbox.example").build());
}
```

Prefer a narrow replacement. Excluding a whole auto-configuration can remove supporting infrastructure you still need.

## Inspect decisions

Run with the debug property or inspect the condition evaluation report:

```bash
java -jar app.jar --debug
```

The report groups positive matches, negative matches, unconditional classes, and exclusions. A negative match is not automatically an error; it often explains a capability not requested by the application.

Actuator's `conditions` endpoint can expose similar data at runtime when deliberately enabled and secured.

## Excluding auto-configuration

```java
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
class ToolApplication { }
```

Or:

```properties
spring.autoconfigure.exclude=\
org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
```

Use exclusion when the capability is genuinely not part of the application, not as the first fix for incomplete database configuration.

## Common mistakes

- Assuming every classpath library creates beans.
- Declaring a replacement with the wrong type so back-off does not trigger.
- Depending directly on an auto-configuration class as public application API.
- Using `@ConditionalOnProperty` for a collection and expecting complex matching.
- Adding `@Order` to auto-configuration instead of supported before/after metadata.
- Reading only startup logs and ignoring the condition report.

## Interview angle

**Interviewer:** A `DataSource` appears locally but not in production. How do you diagnose it?

**Strong answer:** I compare resolved dependencies, effective properties with origins, application type, user bean definitions, and the condition report. An embedded database may satisfy local classpath conditions while production expects an external URL. I make the dependency and configuration contract explicit and add a context test for both environments.

## Quick check

1. When does auto-configuration apply relative to user configuration?
2. What does back-off mean?
3. Why can replacing one bean preserve the rest of a subsystem?
4. What does a negative match prove?
5. When is exclusion justified?

## Predict and debug

**Predict:** A custom `PaymentClient` bean exists before the auto-configuration above. The missing-bean condition fails, so the default client is not registered.

**Debug:** Two clients exist because the custom bean returns a broader interface while the condition checks the implementation class. Align the public bean type/condition contract and test it with `ApplicationContextRunner`.

## Practice

- **Foundation:** Explain a five-line condition report entry in plain language.
- **Interview Core:** Replace one auto-configured bean without excluding the subsystem.
- **Interview Core:** Test enabled, disabled, missing-class, and user-override cases.
- **SDE-2 Follow-up:** Diagnose a classpath-dependent local/production difference.
