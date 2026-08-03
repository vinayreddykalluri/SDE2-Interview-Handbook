# First Application and Project Anatomy

A useful first application is small enough to explain completely. Start with one build file, one application class, one controller, one configuration file, and one test.

## Minimal Maven project

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.1.0</version>
</parent>

<properties>
  <java.version>21</java.version>
</properties>

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

Boot 4.1 prefers the focused `spring-boot-starter-webmvc`; the older `spring-boot-starter-web` is deprecated in its favor. A maintained Boot 3 application commonly still uses `spring-boot-starter-web`.

## Application entry point

```java
package com.example.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

`SpringApplication.run` returns a configured application context. It prepares the environment, determines the application type, loads initializers and listeners, creates the context, registers configuration, refreshes the context, starts the web server when applicable, runs startup callbacks, and publishes lifecycle events.

## First endpoint

```java
package com.example.orders.api;

import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class StatusController {
    @GetMapping("/api/status")
    StatusResponse status() {
        return new StatusResponse("ready", Instant.now());
    }
}

record StatusResponse(String status, Instant observedAt) { }
```

The return value becomes JSON because MVC infrastructure and a JSON message converter are available. The annotation alone does not serialize anything; registered MVC components inspect it.

Expected response shape:

```json
{
  "status": "ready",
  "observedAt": "2026-07-30T12:00:00Z"
}
```

## Run and package

```bash
./mvnw spring-boot:run
./mvnw clean verify
java -jar target/order-service-1.0.0.jar
```

The Maven wrapper pins the build tool used by developers and CI. The Boot Maven plugin repackages the application with its dependencies and launcher metadata. `java -jar` starts the same application entry point without requiring a separately installed servlet container.

## Project anatomy

```text
order-service/
  pom.xml
  mvnw
  .mvn/wrapper/
  src/main/java/com/example/orders/OrderApplication.java
  src/main/java/com/example/orders/api/StatusController.java
  src/main/resources/application.yml
  src/test/java/com/example/orders/OrderApplicationTests.java
```

Keep the application class in a root package above application components. Its package becomes an important default for component, entity, and repository discovery.

## What a starter does not do

A starter contributes a curated dependency graph. It does not guarantee that an auto-configuration matches, that an external server is reachable, or that production settings are correct. Inspect the dependency tree:

```bash
./mvnw dependency:tree
./gradlew dependencies
```

Do not declare versions for libraries managed by Boot without a tested reason. Overriding one component can produce a combination Boot did not test.

## Common mistakes

- Placing the application class in a leaf package, so sibling components are not scanned.
- Adding both servlet and reactive web stacks accidentally.
- Copying dependency versions from random examples.
- Running from the IDE but never verifying the packaged jar.
- Treating a successful HTTP response as proof that readiness and shutdown are correct.

## Interview angle

**Interviewer:** What does `@SpringBootApplication` give you?

**Strong answer:** It combines a configuration class, component scanning, and enablement of Boot auto-configuration. The package location defines default discovery boundaries. `SpringApplication.run` prepares the environment and refreshes the appropriate context; dependencies and conditions decide which infrastructure is registered.

## Quick check

1. Why use a Maven or Gradle wrapper?
2. What does the Boot plugin add to packaging?
3. Why does the package of the application class matter?
4. What turns a controller return value into JSON?
5. Why inspect the dependency tree?

## Practice

- **Foundation:** Create `/api/status` and verify it from the packaged jar.
- **Foundation:** Move the controller outside the root package and explain the resulting 404.
- **Interview Core:** Explain every line of the application class.
- **Interview Core:** Compare a starter, an auto-configuration, and the embedded server.
- **SDE-2 Follow-up:** Write a CI gate that proves tests, packaging, and process startup.
