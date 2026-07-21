# Storage Selection and Consistency

## Selection matrix

| Workload characteristic | Common starting point | Main question |
| --- | --- | --- |
| Relational invariants and joins | Relational database | Which operations need one transaction? |
| Key-based massive scale | Key-value store | Can access remain key-centric? |
| Flexible aggregate documents | Document store | Are aggregate boundaries stable? |
| Relationship traversal | Graph store | Are multi-hop traversals dominant? |
| Time-ordered metrics/events | Time-series or log store | What retention and aggregation? |
| Full-text relevance | Search index | What is authoritative and what is the lag budget? |

## Transaction reasoning

Define the invariant, then identify the smallest atomic boundary. Explain dirty reads, non-repeatable reads, phantoms, lost updates, write skew, optimistic versus pessimistic control, and retry interaction.

## Consistency questions

- Does the user need linearizable, read-your-writes, monotonic, causal, or eventual behavior?
- What happens during a partition or replica lag?
- Can reservation, version checks, compensation, or reconciliation preserve correctness?
- Which data is authoritative, derived, cached, or searchable?

## Drill

Model inventory reservation, account transfer, social likes, and analytics ingestion. Use a different consistency decision for each and defend it from user-visible requirements.
