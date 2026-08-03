# Spring Security: From HTTP Request to an Authorized Method

Security is not “add login.” It is a pipeline that establishes trustworthy context and then applies policy at every sensitive boundary.

## Four ideas before configuration

| Idea | Plain-language meaning | Typical Spring Security representation |
|---|---|---|
| Principal | Who the caller claims to be | `Authentication.getPrincipal()` |
| Credential | Evidence used to prove the claim | Password, session, bearer token, mTLS evidence |
| Authentication | The result of verifying identity evidence | An authenticated `Authentication` |
| Authorization | A decision about an action on a resource | Request rules, `AuthorizationManager`, method security |

Roles and authorities are inputs to policy, not the policy itself. “Has `ADMIN`” may be enough for an internal tool; “may refund this order for this tenant up to this amount” needs resource-aware business rules.

## Servlet request flow

Spring Security’s servlet support is a set of filters behind a `DelegatingFilterProxy`. The proxy delegates into Spring-managed `FilterChainProxy`, which selects the first matching `SecurityFilterChain`.

```text
HTTP request
    |
container filter chain
    |
DelegatingFilterProxy
    |
FilterChainProxy
    |
first matching SecurityFilterChain
    |
load SecurityContext
 -> exploit protection
 -> authentication filter(s)
 -> exception translation
 -> authorization
    |
controller -> service -> repository
```

Two ordering rules explain many bugs:

1. Only the **first matching** security chain is selected. A broad matcher placed first can shadow a specific API chain.
2. Authentication must happen before a rule that needs the authenticated principal.

Do not memorize every built-in filter position. Learn the events: context loading, protection, authentication, exception translation, authorization.

### Minimal version-labeled configuration

For the Spring Security 7 generation, explicit lambdas make intent visible:

```java
@Bean
SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
    return http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/api/public/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/orders/*/refund")
                        .hasAuthority("order:refund")
                    .anyRequest().authenticated())
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
            .build();
}
```

This is intentionally not a complete production policy. It does not decide whether the caller owns a particular order, whether the tenant matches, or whether the amount exceeds an approval limit.

## Authentication internals

A username/password flow makes the contracts easy to see:

```text
filter extracts credentials
        |
        v
AuthenticationManager
        |
        v
ProviderManager tries compatible AuthenticationProvider(s)
        |
        +-> user lookup / credential verification
        |
        v
authenticated Authentication
        |
SecurityContextHolder for the current request
```

The raw password should be verified by a one-way adaptive password encoder. It should not be decrypted. On successful login, change-sensitive operations may require session fixation protection and CSRF protection depending on the client model.

For a bearer-token resource server, the service normally validates signature, issuer, audience, time claims, and maps claims to authorities. A signed token is not automatically acceptable: it can be issued by the wrong issuer, intended for another audience, expired, not-yet-valid, or missing the tenant claim needed by the application.

## Session, bearer token, and context lifetime

| Model | Identity state | Main risk |
|---|---|---|
| Server session | Session identifier points to server-held security context | Session theft/fixation, distributed session consistency |
| Self-contained bearer token | Each request carries signed claims | Revocation delay, incorrect audience/issuer validation, claim over-trust |
| Opaque token | Resource server introspects or validates through an authority | Authority dependency and latency |
| mTLS plus application identity | Transport proves a certificate; app maps it to policy | Certificate lifecycle and confusing transport identity with user intent |

`SecurityContextHolder` is traditionally thread-bound in servlet applications. Starting asynchronous work does not mean the context safely follows. Prefer passing the minimum immutable identity/tenant data needed by the use case, or use a framework-supported context propagation strategy and test it. Never assume arbitrary executor threads inherit request state.

## Authorization at two boundaries

Request authorization rejects obviously forbidden routes early. Method authorization protects application operations even when they acquire a second caller later.

```java
@PreAuthorize("hasAuthority('order:refund')")
public RefundReceipt refund(OrderId orderId, Money amount, Actor actor) {
    Order order = repository.require(orderId);
    policy.requireRefundAllowed(actor, order, amount);
    return refundProcessor.refund(order, amount);
}
```

The annotation checks a coarse capability. `policy.requireRefundAllowed` evaluates object ownership, tenant, state, and monetary rules. That domain decision should be testable without starting Spring.

Method security commonly uses a proxy. The familiar proxy boundaries still apply: the object must be managed, infrastructure must be enabled, and self-invocation may bypass interception. Do not rely on one annotation as the only protection around a privileged external side effect.

## CSRF, CORS, and headers are different concerns

**CSRF** exploits a browser automatically attaching credentials such as cookies. If a browser authenticates with a session cookie, state-changing requests generally need CSRF protection. A stateless API that accepts only an `Authorization` bearer header does not receive that header automatically from a hostile site, but disabling CSRF must follow the actual credential model rather than the word “REST.”

**CORS** is a browser rule controlling which origins may read or send selected cross-origin requests. It is not authentication and does not stop curl or another server.

**Security headers** reduce browser attack surface, but they do not sanitize business data or replace output encoding and content-security design.

## OAuth 2.0 and OpenID Connect without role confusion

- OAuth 2.0 delegates authorization to access protected resources.
- OpenID Connect adds an identity layer and ID tokens for client login.
- A resource server validates access tokens.
- A client obtains/uses tokens to call a resource server.
- An authorization server issues tokens after the relevant grant and user/client checks.

Do not send an ID token to an API as if it were an access token. Do not implement an authorization server inside each business service. Use Authorization Code with PKCE for public/browser-native clients; avoid obsolete password-based grants.

## Failure and edge-case matrix

| Scenario | Naive expectation | Actual risk | Strong response |
|---|---|---|---|
| Broad chain matcher before narrow chain | Both chains combine | First matching chain wins | Order and test representative paths |
| Valid JWT with wrong audience | Signature means trusted | Token was minted for another API | Validate issuer, audience, times, algorithm policy |
| Method calls another secured method on `this` | Both annotations run | Self-invocation may bypass proxy | Move boundary, call through collaborator, test denied path |
| Async task reads current user | Context follows work | Thread-local context is absent/stale | Pass explicit actor data or proven propagation |
| CORS allows origin | Caller is authorized | CORS is only browser access policy | Authenticate and authorize independently |
| CSRF disabled for cookie login | API is “stateless” | Browser still sends cookie automatically | Preserve token-based CSRF defense or redesign credentials |
| `permitAll` endpoint logs raw token | Public route is harmless | Secret leaks to logs | Redact headers and payloads by policy |
| `ROLE_ADMIN` across tenants | Admin can manage all | Cross-tenant data exposure | Combine authority with tenant/resource checks |
| Cached permission after revocation | Cache improves latency | Stale authorization window | Define TTL/invalidation and critical-operation recheck |

## Debugging sequence

When an endpoint unexpectedly returns 401 or 403:

1. Confirm which `SecurityFilterChain` matched.
2. Print or inspect the filter chain in a safe environment.
3. Distinguish authentication failure (often 401) from an authenticated-but-denied decision (often 403).
4. Inspect issuer, audience, expiry, clock skew, and authority mapping—not the raw token in shared logs.
5. Confirm request matcher semantics and HTTP method.
6. Confirm method-security proxy crossing and resource policy.
7. Add one allowed and one denied integration test.

## Quick check

1. Why can a valid signature still produce an unacceptable JWT?
2. What chooses among multiple servlet security chains?
3. Why is CORS not access control?
4. Where should tenant-aware authorization live?
5. Why can async execution lose the current security context?

## Practice

- **Foundation:** Draw authentication and authorization as separate steps for session login.
- **Interview Core:** Diagnose why `/api/admin/**` is public when two filter chains exist.
- **Interview Core:** Write a policy interface for tenant-aware order refunds and test it in plain Java.
- **SDE-2 Follow-up:** Design revocation behavior for a 15-minute bearer token used by a high-risk payment endpoint.
