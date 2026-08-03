# Security Configuration and Production Architecture

Spring Security deserves its own book. This chapter defines the Boot-level responsibilities every backend engineer must understand: secure defaults, management-surface protection, forwarded headers, CORS/CSRF boundaries, secrets, dependency risk, and production configuration.

## Adding Security changes behavior

When Security is on the classpath, Boot can register a default security setup only while the application has not supplied its own relevant beans. Defining a `SecurityFilterChain` causes parts of that default to back off, so the application must then authorize the complete surface deliberately.

```java
@Bean
SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
    return http
            .authorizeHttpRequests(requests -> requests
                    .requestMatchers("/actuator/health/**").permitAll()
                    .requestMatchers("/actuator/**").hasRole("OPS")
                    .anyRequest().authenticated())
            .build();
}
```

This is illustrative, not a complete authentication design. Browser sessions, token APIs, OAuth2 resource servers, and internal mTLS have different CSRF and credential rules.

## CORS is browser permission

CORS tells a browser whether frontend origin A may call API origin B. It does not authenticate the caller and does not stop server-to-server requests.

Avoid `*` with credentials. Enumerate trusted origins/methods/headers and test preflight behavior through the security filter chain.

## CSRF depends on credential transport

Cookie-based browser authentication is automatically attached by the browser and needs CSRF protection. A stateless API using an authorization header has a different threat model. Do not disable CSRF merely because an endpoint returns JSON.

## Proxy and forwarded headers

Behind a trusted proxy, the application may need original scheme, host, port, and client information. Trust forwarded headers only from controlled proxies; otherwise a client can spoof secure links, redirect targets, rate-limit identity, or audit data.

Define:

- which proxy terminates TLS;
- which headers it overwrites;
- which network may reach the application directly;
- how generated absolute URLs are tested.

## Secrets and diagnostics

Sensitive values can leak through startup errors, environment endpoints, configuration dumps, request logging, heap dumps, thread names, metrics tags, and support bundles. Protect diagnostic endpoints and establish redaction before incidents.

## Production architecture checklist

- separate business API and operator permissions;
- authentication and authorization tested for allow and deny paths;
- TLS and proxy trust documented;
- request/body limits and timeouts configured;
- actuator exposure allowlisted;
- secrets externally delivered and rotated;
- dependency/image scanning tied to patch ownership;
- audit events durable where required;
- security headers and CORS behavior verified at the edge;
- error responses reveal no internals.

## Common mistakes

- Assuming Boot secures custom Actuator exposure automatically.
- Permitting health with a broad matcher that also exposes details.
- Disabling CSRF without identifying credential transport.
- Using CORS as authentication.
- Trusting client-supplied forwarded headers directly.
- Printing configuration during support incidents.
- Hard-coding secrets in `application-prod.yml`.

## Interview angle

**Interviewer:** You added a custom filter chain and Actuator became public. Why?

**Strong answer:** Security auto-configuration backed off when the application supplied its own chain, and the custom rules did not cover the management surface correctly. I define explicit matchers and order for operator versus application endpoints, test unauthorized/authorized paths, restrict network exposure, and verify health detail policy.

## Quick check

1. What can cause security auto-configuration to back off?
2. What does CORS protect?
3. When is CSRF relevant?
4. Why are forwarded headers a trust decision?
5. Which diagnostics may contain secrets?

## Practice

- **Foundation:** Separate public readiness from private health details.
- **Interview Core:** Threat-model CORS and CSRF for cookie and token clients.
- **Interview Core:** Test a management endpoint allow/deny matrix.
- **SDE-2 Follow-up:** Design trusted-proxy handling for two load-balancer layers.
