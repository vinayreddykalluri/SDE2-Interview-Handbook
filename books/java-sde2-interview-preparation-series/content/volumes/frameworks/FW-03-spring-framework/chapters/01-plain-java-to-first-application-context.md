# Plain Java to the First Application Context

Start with working plain Java. Spring should remove assembly repetition, not hide an object model you never understood.

## A plain Java object graph

```java
interface MessageSender {
    void send(String recipient, String text);
}

final class ConsoleMessageSender implements MessageSender {
    @Override
    public void send(String recipient, String text) {
        System.out.println(recipient + ": " + text);
    }
}

final class WelcomeService {
    private final MessageSender sender;

    WelcomeService(MessageSender sender) {
        this.sender = sender;
    }

    void welcome(String email) {
        sender.send(email, "Welcome");
    }
}

public class PlainJavaApplication {
    public static void main(String[] args) {
        MessageSender sender = new ConsoleMessageSender();
        WelcomeService service = new WelcomeService(sender);
        service.welcome("reader@example.com");
    }
}
```

Expected output:

```text
reader@example.com: Welcome
```

`WelcomeService` depends on the interface, not on Spring. Its constructor is a complete statement of required collaborators.

## Add only the container

The minimal Maven dependency is `spring-context`; it brings the core container modules transitively. Pin the version through your build rather than copying a floating version from a blog.

```xml
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-context</artifactId>
  <version>7.0.8</version>
</dependency>
```

Register the same graph through Java configuration:

```java
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ApplicationConfiguration {
    @Bean
    MessageSender messageSender() {
        return new ConsoleMessageSender();
    }

    @Bean
    WelcomeService welcomeService(MessageSender sender) {
        return new WelcomeService(sender);
    }
}

public class SpringApplication {
    public static void main(String[] args) {
        try (var context = new AnnotationConfigApplicationContext(
                ApplicationConfiguration.class)) {
            WelcomeService service = context.getBean(WelcomeService.class);
            service.welcome("reader@example.com");
        }
    }
}
```

Expected output is unchanged. That is a feature: business behavior did not become a framework concern.

## Dry run: what the context does

1. Read `ApplicationConfiguration` as configuration metadata.
2. Register bean definitions for `messageSender` and `welcomeService`.
3. Create the `MessageSender` singleton.
4. Resolve the `MessageSender` parameter of `welcomeService`.
5. Invoke the factory method and store the resulting singleton.
6. Publish context lifecycle events and make the context ready.
7. Return the existing `WelcomeService` when `getBean` is called.
8. On `close`, run eligible destruction callbacks.

Calling `getBean` is useful at the composition root and in demonstrations. Business classes should usually receive dependencies instead of pulling them from the context. Frequent service-locator calls hide dependencies and couple domain code to Spring.

## `BeanFactory` versus `ApplicationContext`

`BeanFactory` is the foundational bean-container contract. `ApplicationContext` extends it and adds application-oriented capabilities such as event publication, resource loading, message resolution, environment access, and automatic detection of many extension beans.

| Question | Preferred answer |
|---|---|
| Which one do applications normally use? | `ApplicationContext` |
| Is `BeanFactory` obsolete? | No; it is the lower-level container contract |
| Does `ApplicationContext` eagerly create every bean? | It pre-instantiates non-lazy singleton beans by default, with exceptions for lazy and other scopes |
| Should business code depend on either? | Usually no |

## Startup failure is valuable

Constructor injection allows missing or ambiguous dependencies to fail during context refresh rather than during a production request.

```java
@Bean
WelcomeService welcomeService(MessageSender sender) {
    return new WelcomeService(sender);
}
// No MessageSender bean -> context refresh fails.
```

Treat startup validation as an operational control. A service that cannot construct its required object graph should fail before receiving traffic.

## Debugging a first context

When startup fails, read from the deepest cause outward:

1. Find `NoSuchBeanDefinitionException`, `NoUniqueBeanDefinitionException`, construction failure, or configuration error.
2. Identify the exact injection point and required type/name/qualifier.
3. List candidate bean definitions, including profiles and imports.
4. Repair the object graph; do not add unrelated component scans.

## Common mistakes

- Creating a second context accidentally and expecting its beans to be shared.
- Calling `new WelcomeService(...)` and expecting Spring advice on that unmanaged object.
- Hiding all construction behind static access.
- Closing a context that another part of the process still owns.
- Starting Spring in every test when a constructor call is enough.

## Interview angle

**Interviewer:** What happens during `ApplicationContext` startup?

**Strong answer:** Spring reads configuration into bean definitions, runs definition-level extension points, instantiates eligible singletons, resolves dependencies, invokes aware and lifecycle callbacks through registered post-processors, may wrap beans in proxies, validates the graph, and publishes refresh lifecycle events. I distinguish definition metadata from actual instances and mention that lazy or non-singleton beans may be created later.

## Quick check

1. Why does the first Spring example preserve ordinary Java constructors?
2. What additional capabilities does `ApplicationContext` provide?
3. When are normal non-lazy singletons created?
4. Why is `getBean` inside business logic usually a smell?
5. Why is a startup wiring failure safer than a late request failure?

## Predict and debug

**Predict:** Two calls to `context.getBean(WelcomeService.class)` return what by default? The same singleton instance for that bean definition.

**Debug:** A developer creates `new WelcomeService(sender)` and reports that an aspect never runs. The object was not obtained through the container, so no Spring proxy surrounds it.

## Practice

- **Foundation:** Convert a plain `ReportService -> Clock` graph to two `@Bean` methods.
- **Foundation:** Print whether two lookups return the same reference.
- **Interview Core:** Explain context refresh in eight steps without using Boot terminology.
- **Interview Core:** Write a unit test for `WelcomeService` with a fake sender and no Spring.
- **SDE-2 Follow-up:** Design startup checks for three required external configuration values without connecting to a production system.

## Readiness checkpoint

Continue when you can build and close an `AnnotationConfigApplicationContext`, distinguish definition from instance, and explain why business classes remain testable without Spring.
