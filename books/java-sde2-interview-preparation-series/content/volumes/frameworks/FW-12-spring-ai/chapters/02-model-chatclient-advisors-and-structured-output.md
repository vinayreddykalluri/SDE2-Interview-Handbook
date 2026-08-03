# Models, ChatClient, Advisors, and Structured Output

## Model fundamentals in application language

A chat model receives an ordered sequence of messages and options, then predicts output tokens. It does not query your database unless your application retrieves data or exposes a tool. It has no durable memory unless prior messages or external memory are supplied again.

Terms to know:

- **Token:** a model-specific unit of text. Character count is not token count.
- **Context window:** the maximum combined input/output token budget supported for a request.
- **Temperature/sampling options:** influence output distribution; they do not create correctness guarantees.
- **System/developer instruction:** application-supplied guidance. It can still be attacked by untrusted content.
- **User message:** task input, always untrusted.
- **Finish reason:** why generation stopped, such as normal completion or length limit; provider vocabulary varies.

A low temperature can reduce variation for some providers/models, but it does not turn the model into a deterministic function or remove hallucination.

## Spring AI abstraction layers

```text
application service
      |
ChatClient fluent request
      |
ordered Advisor chain
      |
ChatModel abstraction + provider options
      |
provider SDK / HTTP API
      |
model response + usage metadata
```

Use the lower-level `ChatModel` when you need direct model control. Use `ChatClient` for a fluent application-facing API, defaults, advisors, entity conversion, and call/stream integration.

## A minimal Spring AI 2.0-style request

```java
record SupportAnswer(String answer, List<String> sourceIds, boolean abstained) {}

SupportAnswer answer = chatClient.prompt()
        .system("Answer only from supplied support evidence. "
                + "If evidence is insufficient, set abstained=true.")
        .user(userMessage)
        .call()
        .entity(SupportAnswer.class);
```

The exact provider and starter configuration belongs in Boot configuration. Never commit an API key or expose it as a browser credential.

`.entity(...)` converts structured model output to a Java type. Depending on provider/model and options, the schema may be conveyed as instructions or by provider-native structured output. Provider-native support and limitations vary. In Spring AI 2.0, schema validation and bounded repair/repeat capabilities are available through the current `ChatClient` entity configuration; verify exact method names against the managed version.

## Structured output has three validation layers

```text
model text
  -> syntax/schema validation
  -> Java conversion
  -> business validation
  -> authorization/effect boundary
```

For a proposed refund:

```java
record RefundProposal(String orderId, long amountInCents, String reason) {}
```

JSON schema can require the fields and numeric type. It cannot prove that the order exists, belongs to the tenant, is refundable, or that the caller has permission. Those checks belong in application code. Never deserialize model output into an entity and save it directly.

## Advisor chain mechanics

Advisors can transform a request, add context, implement memory/retrieval/tool patterns, call the next advisor/model, inspect a response, or fail the call.

```text
user request
  -> safety/size advisor
  -> memory advisor
  -> retrieval advisor
  -> tool-calling advisor
  -> model
  <- response validation/observation unwind
```

Order matters. A retrieval advisor that runs before memory-based question rewriting may search for “What about its warranty?” without knowing what “its” means. A logger placed after context augmentation can record sensitive retrieved documents. The Spring AI 2.0 advisor contracts distinguish synchronous call and streaming paths; test both if the feature exposes both.

Advisors share per-request context through supported advisor context, not through mutable singleton fields. A singleton advisor with per-user mutable state creates race and tenant-leak risks.

## Call versus stream

`call()` waits for the complete result. A streaming call returns chunks/signals as they arrive. Streaming improves time-to-first-token but complicates contracts:

- the final schema may be incomplete until the stream ends;
- policy checks that require the whole answer cannot approve earlier chunks retroactively;
- client disconnect should cancel work where possible;
- token usage/final finish metadata may arrive only at completion;
- a mid-stream provider failure leaves a partial user-visible answer.

Use buffering when complete validation is mandatory. If you stream, label unvalidated partial output and design a terminal/error event protocol.

## Prompt construction

Keep instructions and data distinguishable:

```text
SYSTEM: allowed task, evidence rules, output contract, refusal behavior
USER: untrusted question
CONTEXT: delimited retrieved documents with IDs and trust labels
```

Do not concatenate user text into an instruction sentence such as:

```java
"Always obey this request: " + userText
```

Templates improve consistency, but placeholders do not sanitize prompt injection. Limit sizes, delimit sources, assign trust, and enforce policy after the model.

## Context-window and truncation policy

Input includes system instructions, conversation, tool definitions/results, retrieved chunks, and user text. Leave output reserve.

```text
model context limit
 - system/tool overhead
 - requested output reserve
 = maximum request evidence/history budget
```

Silent truncation can remove the instruction or evidence that makes an answer safe. Define a deterministic priority: preserve core policy, current user task, necessary tool schema, highest-quality evidence, then the most relevant recent history. Reject or summarize through a separately evaluated path when the budget cannot fit.

## Failure and edge-case matrix

| Scenario | Hidden failure | Response |
|---|---|---|
| Valid JSON | Invented IDs/amounts | Business and authorization validation |
| Low temperature | Still wrong/variable | Evaluation and grounding |
| Advisor order changes | Retrieval/memory semantics change | Explicit order and regression tests |
| Mutable singleton advisor | Cross-request leak/race | Request context and immutable configuration |
| Stream parsed as final entity | Incomplete JSON | Buffer or use a streaming event contract |
| Provider stops at token limit | Truncated answer/schema | Finish-reason handling and output reserve |
| Schema-repair loop | Latency/cost grows | Bounded repeats inside total deadline |
| Huge user prompt | Cost/availability attack | Size/token limit before provider call |
| Raw prompt logging | Secrets/PII leak | Redaction, sampling, restricted access |
| Provider-specific option on another model | Ignored/rejected semantics | Capability detection and provider contract tests |

## Quick check

1. What is counted inside a context window?
2. Why does `.entity(...)` not prove a result is safe to execute?
3. How can advisor order change retrieval quality?
4. Why is streaming incompatible with some full-response validators?
5. Why is a low temperature not a correctness guarantee?

## Practice

- **Foundation:** Draw the request/response path from service to provider.
- **Interview Core:** Define schema and business validation for a ticket-routing result.
- **Interview Core:** Allocate a context budget among policy, tools, history, retrieval, and output.
- **SDE-2 Follow-up:** Design advisor ordering for memory-aware retrieval without logging sensitive context.
