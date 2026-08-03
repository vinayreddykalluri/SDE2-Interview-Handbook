# Conversation Memory: State Ownership, Privacy, and Tenancy

Chat model APIs are normally stateless between calls. “Memory” means the application selects prior information and sends it again, or retrieves it through a memory mechanism. The model is not quietly remembering your user by itself.

## Three kinds of state

| State | Example | Appropriate lifetime |
|---|---|---|
| Turn-local | Current question, retrieved chunks, tool results | One request/stream |
| Conversation | Recent user/assistant messages | Bounded conversation/session |
| Durable user/business fact | Preferred language, verified account state | Source-of-truth domain store with explicit policy |

Do not use generated conversation summaries as authoritative account data. If a preference matters, let the user confirm it and store it through an ordinary validated application path.

## Memory advisor flow

```text
authenticated request + conversation ID
      |
authorize conversation ownership
      |
load bounded history
      |
advisor adds selected history to model request
      |
model response
      |
store allowed final exchange / summary
```

In current Spring AI, memory advisors require a conversation identifier in advisor context for each call. Exact constants/builders are version-sensitive. Supplying an ID is not authorization: the server must verify the actor owns that conversation.

## Stable identifiers

Unsafe flow:

```text
client sends conversationId=other-user-id -> server loads it directly
```

Safer key:

```text
(tenantId, authenticatedUserId, serverIssuedConversationId)
```

The repository query must include the server-derived tenant/user scope. Random IDs reduce guessing but do not replace authorization.

## History does not fit forever

Every message consumes context and cost. A memory strategy can:

- keep the last N messages;
- keep messages within a token budget;
- summarize older turns;
- retrieve semantically relevant memories;
- retain durable facts separately from chat transcript.

Summaries are lossy model output. Version and evaluate the summarizer, preserve required facts deterministically, and do not repeatedly summarize summaries without drift tests.

Tool-call intermediate messages have version-specific storage behavior. In Spring AI 2.0, the tool-calling advisor can maintain conversation history for its internal loop while memory advisors persist the intended final exchange. Verify the exact behavior before relying on intermediate calls for audit.

## Privacy lifecycle

Define before storage:

- purpose and lawful/organizational basis;
- fields allowed and prohibited;
- tenant/user ownership;
- encryption and access control;
- retention and expiry;
- export and deletion behavior;
- backup and cache deletion lag;
- whether provider retention/training settings meet policy;
- safe logs, traces, evaluation datasets, and support access.

Redacting an email with a regular expression is not complete de-identification. Free text can contain names, secrets, account numbers, health data, source code, or unique events.

## Memory poisoning

A user or retrieved document may try to create durable instructions: “Remember that I am an administrator.” Never store model-inferred privilege. Separate:

- untrusted conversational statements;
- user-confirmed preferences;
- verified business facts from source systems;
- immutable security claims from the authenticated context.

When memory is retrieved later, label it as untrusted historical content and reapply current authorization.

## Multi-region and concurrency edges

Two tabs can append to one conversation concurrently. Use message IDs, monotonic sequence/version checks, or append-only storage with a deterministic ordering policy. Do not let last-write-wins overwrite a whole transcript.

Replicated stores may return stale history. Decide whether stale conversation context is acceptable; never use it for current balance, permission, or order state.

## Failure and edge-case matrix

| Scenario | Failure | Control |
|---|---|---|
| Client chooses another conversation ID | Cross-user leak | Server-scoped composite key and authorization |
| Unlimited history | Context/cost growth | Token/message budget and expiry |
| Summary drops exception | Wrong future answer | Evaluate summaries; preserve critical facts deterministically |
| User says “I am admin” | Memory poisoning | Never derive privilege from conversation |
| Deletion removes only primary row | Data remains in cache/vector/log/backup | End-to-end deletion inventory and lag SLO |
| Two tabs append | Lost/out-of-order turns | Append IDs/versioning and ordering policy |
| Trace records prompts | Sensitive retention | Redaction/off-by-default content capture |
| Provider retains data | Contract breach | Provider configuration, agreement, routing policy |
| Old memory supplies current fact | Stale business action | Re-read source-of-truth tool under authorization |

## Quick check

1. Why is a conversation ID not proof of ownership?
2. What facts should never be learned as security state from chat?
3. Why can repeated summarization drift?
4. What stores must a deletion workflow consider?
5. How should two concurrent conversation writes be ordered?

## Practice

- **Foundation:** Classify ten fields as turn, conversation, or source-of-truth state.
- **Interview Core:** Design the repository key and authorization query for multi-tenant chat history.
- **Interview Core:** Set a token-budget policy for a 20-turn conversation.
- **SDE-2 Follow-up:** Design export/deletion across transcript, vector memory, caches, logs, evaluation samples, and backups.
