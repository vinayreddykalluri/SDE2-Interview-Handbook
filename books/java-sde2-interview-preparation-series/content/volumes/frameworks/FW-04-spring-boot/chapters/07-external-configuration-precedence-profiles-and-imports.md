# External Configuration, Precedence, Profiles, and Imports

External configuration lets one immutable artifact run in multiple environments. The difficult part is not YAML syntax; it is predicting which value wins, where it came from, and whether changing it is safe.

## Values have origins and precedence

Boot creates an ordered environment. Later, higher-precedence sources override earlier ones. Important groups include:

```text
defaults and packaged config
        < profile-specific packaged config
        < external config files
        < profile-specific external config
        < environment variables / system properties
        < command-line arguments
        < test-specific overrides
```

This is a learning model, not a substitute for the complete official order. JSON application properties, servlet configuration, Devtools, and test annotations occupy defined positions too. When diagnosing, inspect the property source and origin instead of relying on a memorized partial list.

## File locations

Boot loads `application.properties` or YAML from conventional classpath and external locations. External directories can override packaged defaults. Additional locations and names can be supplied, but changing location properties happens very early and can make startup depend on launch commands.

```yaml
server:
  port: 8080
payment:
  base-url: https://payments.example
  timeout: 2s
```

Environment variable names use relaxed binding conventions, for example `PAYMENT_BASEURL` or platform-specific mapped forms. Prefer testing the actual deployment mapping; shell and orchestrator behavior can differ.

## Profiles select groups of configuration

```yaml
spring:
  profiles:
    group:
      production:
        - cloud
        - observability
```

A profile is a named configuration selection, not a security boundary. Avoid profiles such as `customer-a`, `customer-b`, and dozens of environment permutations. Use typed properties and deployment configuration for values; reserve profiles for coherent feature/configuration groups.

Profile activation cannot be defined inside a document that is itself conditional on that profile. Bootstrap decisions must be available before the document is selected.

## Configuration imports

```properties
spring.config.import=optional:file:/etc/order-service/overrides.properties
```

Imports can bring additional configuration into the environment at a precise location. The `optional:` prefix controls whether absence is fatal. Use non-optional imports when missing configuration makes the service unsafe.

Config trees map files in a mounted directory to properties, which is useful for container secrets:

```properties
spring.config.import=configtree:/run/secrets/
```

Do not log the resulting secret values or expose them through Actuator.

## Placeholders and random values

```properties
order.region=${ORDER_REGION:local}
order.instance-id=${HOSTNAME:unknown}
```

Defaults are useful only when they are safe. A default production database password or payment endpoint is not safe. `RandomValuePropertySource` is useful for tests and non-secret identifiers; it is not a secret-management system.

## Diagnose one value

1. Write the canonical property name.
2. List all candidate sources.
3. Identify active profiles and imported documents.
4. Inspect the winning source and origin.
5. Confirm the bound target and conversion.
6. Remove accidental duplicates.

Actuator `env` and `configprops` endpoints sanitize values by default, but endpoint access still requires a security decision.

## Common mistakes

- Assuming YAML wins over environment variables.
- Using profiles as tenant or feature-flag storage.
- Making a critical config import optional.
- Committing secrets to source control.
- Defining the same property in many places without ownership.
- Copying a property from a different Boot version.
- Assuming configuration reload occurs automatically.

## Interview angle

**Interviewer:** Production ignores the value in `application.yml`. Why?

**Strong answer:** I inspect active profiles, external files and imports, environment/system/command-line values, relaxed name mapping, and the property origin. The packaged YAML is only one source. I also verify the value bound to the target type and whether the deployment injects an empty or malformed value.

## Quick check

1. Why are value origin and precedence inseparable?
2. What is a profile appropriate for?
3. What does `optional:` change for an import?
4. Why is a random property not a secret?
5. Does Boot reload changed files automatically?

## Practice

- **Foundation:** Predict winners for packaged, external, environment, and CLI values.
- **Foundation:** Convert three properties to environment-variable names.
- **Interview Core:** Design a fail-fast secret import.
- **Interview Core:** Replace twelve environment profiles with typed configuration.
- **SDE-2 Follow-up:** Produce a configuration provenance runbook that never prints secrets.
