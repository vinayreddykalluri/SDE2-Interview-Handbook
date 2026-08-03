# Retrieval-Augmented Generation: Evidence Before Eloquence

RAG retrieves application-owned evidence and places selected context into a model request. It can improve freshness, domain relevance, and citations. It does not guarantee that retrieval found the right source or that the model used it faithfully.

## Two separate pipelines

### Ingestion

```text
approved source
  -> parse
  -> normalize
  -> split into chunks
  -> attach metadata and authorization scope
  -> embed
  -> upsert vector + text + metadata + source version
```

### Query

```text
authenticated question
  -> derive mandatory tenant/ACL filter
  -> optional query rewrite
  -> embed/search (vector, keyword, or hybrid)
  -> rerank/deduplicate
  -> evidence threshold and token budget
  -> grounded prompt
  -> answer + cited source IDs
  -> verify citations/abstain
```

Treat ingestion and query as separately observable, versioned systems. A perfect query cannot find a document that failed ingestion.

## Embedding and similarity intuition

An embedding converts content to a numeric vector. Semantically related text often has vectors that are near under the chosen similarity function.

Cosine similarity compares direction:

```text
cosine(a, b) = dot(a, b) / (length(a) * length(b))
```

Higher is usually more similar under cosine, but score meaning depends on model, store, index, metric, and corpus. A threshold copied from another system is not evidence.

Embedding models define vector dimension and semantics. Changing the model generally requires re-embedding the corpus or maintaining a versioned parallel index; vectors from incompatible models should not share one search space.

## Chunking is a retrieval decision

Chunks that are too small lose definitions and exceptions. Chunks that are too large dilute the relevant passage and consume context. Good chunking respects document structure: title, section, paragraphs, tables, code blocks, and policy clauses.

Store metadata such as:

- stable source and chunk ID;
- tenant/ACL scope;
- document version and effective date;
- title/section/path;
- language/content type;
- ingestion timestamp and embedding model version;
- deletion/tombstone state;
- trust/authority level.

Overlap can preserve context across boundaries but duplicates results and tokens. Deduplicate adjacent or near-identical hits before prompt assembly.

## Authorization before retrieval

Never retrieve broadly and ask the model to ignore forbidden chunks.

```text
authenticated actor
      |
derive server-side tenant + ACL filter
      |
vector/text search executes within allowed scope
      |
only authorized evidence can enter prompt
```

A tenant ID supplied by the user is not trustworthy. Bind the filter from the authenticated application context. Verify the selected vector-store connector actually enforces metadata filters as expected; use separate indexes/collections when the risk requires stronger isolation.

## Retrieval strategies

| Strategy | Strength | Weakness |
|---|---|---|
| Vector | Semantic paraphrases | Exact identifiers/numbers may rank poorly |
| Keyword/BM25 | Exact names, codes, rare terms | Vocabulary mismatch |
| Hybrid | Combines semantic and lexical evidence | More tuning and latency |
| Metadata filter | Enforces scope/time/type constraints | Bad metadata silently removes/admits sources |
| Reranker | Improves ordering of candidates | Extra latency/cost and another model dependency |

Query rewriting can resolve conversation references, but a rewrite can change intent. Preserve the original question, inspect both in evaluation, and never let the model rewrite server-owned authorization filters.

## Grounding and citation verification

Assign every context chunk an opaque source ID. Ask the model to cite IDs, then verify:

1. every cited ID was actually supplied;
2. the answer’s key claims have cited evidence;
3. the evidence contains support rather than merely a related topic;
4. below-threshold or conflicting evidence triggers abstention/escalation.

Citation presence is easy to game; citation correctness needs evaluation and sometimes deterministic claim/source checks.

## Freshness, update, and deletion

An update can leave old and new chunks together. Use versioned source IDs and replace atomically where the store permits, or query only the active version. A deletion request must remove source text, vectors, caches, conversation artifacts, and derived evaluation examples according to policy.

Track ingestion lag. “The source document changed” is not the same as “the searchable index is current.”

## Failure and edge-case matrix

| Failure | Symptom | Detection/response |
|---|---|---|
| Wrong tenant filter | Cross-tenant citation/leak | Negative isolation tests and server-derived filter |
| Missing ingestion | Model says no policy exists | Source-to-index reconciliation and freshness SLO |
| Duplicate old/new chunks | Conflicting answer | Versioned replace/tombstone and active-version filter |
| Identifier query | Vector misses exact order/code | Hybrid/keyword path |
| Huge chunks | Relevant source ranks but answer drifts | Structure-aware smaller chunks and reranking |
| Tiny chunks | Exception clause separated | Parent/neighbor context or semantic sections |
| Threshold copied globally | Good questions abstain or bad results pass | Calibrate per corpus/task on evaluation set |
| Embedding model changed | Similarity quality collapses | Parallel versioned re-index and migration gate |
| Prompt budget exceeded | Best evidence truncated | Deterministic ranking/dedup/budget allocation |
| Malicious retrieved text | Context tells model to leak/call tool | Treat documents as untrusted; isolate tools and post-validate |

## Evaluate retrieval separately from generation

For questions with known relevant chunks, measure:

- recall@k: did top-k contain required evidence?
- precision@k: how much retrieved evidence was relevant?
- rank of first relevant chunk;
- authorization violations (target zero);
- freshness lag and ingestion failures;
- no-answer calibration.

Only after retrieval is acceptable should you score answer groundedness and usefulness. Otherwise a fluent answer can hide a broken index.

## Quick check

1. Why does changing embedding model require an index migration?
2. Why must tenant filtering happen before prompt construction?
3. When is keyword retrieval stronger than vector retrieval?
4. What is the difference between citation presence and citation correctness?
5. How can chunk overlap harm a prompt?

## Practice

- **Foundation:** Split one policy page into chunks and defend the boundaries.
- **Interview Core:** Design metadata for a multi-tenant support corpus.
- **Interview Core:** Create five gold questions with expected source IDs and no-answer cases.
- **SDE-2 Follow-up:** Plan a zero-downtime migration from embedding model A to B with rollback evidence.
