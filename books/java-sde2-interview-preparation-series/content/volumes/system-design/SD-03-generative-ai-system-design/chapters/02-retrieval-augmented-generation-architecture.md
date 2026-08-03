# 2. Retrieval-Augmented Generation: Architecture and Failure Modes

## Learning objectives

By the end of this chapter, you should be able to:

- draw the full RAG pipeline and name what can fail at every stage;
- choose a chunking strategy and defend it against the retrieval quality it produces;
- explain approximate nearest neighbour search, the recall/latency trade-off, and when exact search is correct;
- combine lexical and semantic retrieval, and say why hybrid usually beats either alone; and
- diagnose a "the answer was wrong" report down to the specific stage that caused it.

## Why this matters at SDE-2

RAG is the most common generative-AI design prompt because it is the most common production shape: a model that must answer over data it was never trained on, with citations, without retraining.

The reason it makes a good interview question is that it is a **distributed data problem wearing an AI costume**. Ingestion, chunking, indexing, ranking, freshness, and access control are all classic backend concerns. Candidates who fixate on the model miss that most RAG failures happen before the model is called, and the ones that matter most are access-control failures.

## First-principles model

RAG exists because of a mismatch. A model's knowledge is frozen at training time and has no notion of your data or your permissions. Fine-tuning is expensive, slow to update, and cannot enforce authorization. Retrieval sidesteps all three: fetch the relevant text at request time and put it in the prompt.

The pipeline has two halves that run on completely different schedules:

```text
INGESTION (offline, batch or streaming)
  source -> extract -> chunk -> embed -> index (+ metadata, + ACL)

QUERY (online, per request)
  question -> embed -> search -> filter by ACL -> rerank -> assemble prompt -> generate -> cite
```

The central insight, and the one to state early in an interview: **the model can only be as good as the retrieval.** If the correct passage is not in the top-k, no amount of prompt engineering recovers it. The model will answer anyway, fluently and wrongly. So the engineering effort belongs in retrieval quality, not in prompt wording.

The second insight: **retrieval is a ranking problem, and ranking is measurable.** Recall@k, precision@k, and mean reciprocal rank are the metrics. A team that cannot state its recall@5 is guessing.

> **Specification boundary:** No standard defines chunking, embedding, or index behavior. Embedding models are versioned artifacts whose vectors are not comparable across versions - changing the model invalidates every stored vector and requires a full re-index. Treat the embedding model as a schema, and changing it as a migration.

## Core terminology

- **Chunk:** a unit of text indexed and retrieved as a whole.
- **Embedding:** a dense vector representing text meaning; comparable only within one model version.
- **ANN (approximate nearest neighbour):** sublinear vector search that trades exactness for speed.
- **HNSW:** a graph-based ANN index; high recall, high memory, fast queries.
- **IVF:** a partition-based ANN index; lower memory, tunable via probe count.
- **Recall@k:** fraction of truly relevant chunks appearing in the top k results.
- **BM25:** a lexical ranking function; strong on exact terms, identifiers, and rare words.
- **Hybrid search:** combining lexical and semantic results into one ranking.
- **RRF (reciprocal rank fusion):** a score-free method of merging ranked lists.
- **Reranker:** a cross-encoder scoring query and document jointly; slow, accurate, applied to a shortlist.
- **Grounding:** constraining the answer to the retrieved context.
- **Citation:** the mapping from a generated claim back to its source chunk.

## Detailed mechanics

### Chunking is the decision that constrains everything downstream

Chunking is where most quality is won or lost, and it is usually treated as an afterthought.

The tension is simple. Chunks that are too small lose the context needed to be interpretable - a paragraph that says "this limit is 500 per hour" is useless without knowing which limit. Chunks that are too large dilute the embedding, so a document about twelve topics has a vector that represents none of them well, and they waste context window at query time.

| Strategy | How it works | Use when |
|---|---|---|
| Fixed-size with overlap | N tokens, sliding by N minus overlap | Homogeneous prose; the safe default |
| Structural | Split on headings, sections, list items | Documentation, legal, anything with real structure |
| Semantic | Split where embedding similarity drops | Unstructured text where structure is absent |
| Whole document | No splitting | Short documents already under the budget |

Practical guidance: start at 400 to 800 tokens with 10 to 15 percent overlap, and split on structural boundaries when the source has them. Overlap exists so a fact spanning a boundary is not truncated in both neighbours.

Two refinements matter more than tuning the size:

**Contextual headers.** Prefix each chunk with its document title and section path before embedding. A chunk reading "The limit is 500 per hour" embeds far better as "API Reference > Rate Limits > Free Tier: The limit is 500 per hour". This is cheap and routinely produces a larger quality gain than switching embedding models.

**Store the parent.** Embed and search on small precise chunks, but pass the surrounding parent section to the model. You get retrieval precision and generation context at once.

### Vector search and the recall/latency trade-off

Exact nearest-neighbour search is O(n) per query. At 400,000 vectors that is fine; at 50 million it is not. ANN indexes trade guaranteed exactness for sublinear search.

**HNSW** builds a navigable small-world graph. Fast queries, high recall, but holds the graph in memory and is expensive to update. Tuning: `M` (connections per node) and `efSearch` (candidates explored) - raising `efSearch` raises recall and latency together.

**IVF** partitions vectors into clusters and probes only the nearest few. Lower memory, and `nprobe` gives a direct recall/latency dial. Quantization (PQ) shrinks memory further at some accuracy cost.

The engineering point for an interview: **ANN recall is a tunable you must measure, not a property you inherit.** An index at 85% recall silently drops the right answer for 15% of queries, and the system reports no error. Build a labelled query set and measure recall@k before and after any index change.

When exact search is right: under roughly a million vectors, or when a missed result is a correctness failure rather than a quality one. Do not reach for a distributed vector database because the problem sounds large; compute the actual index size first.

### Metadata filtering and access control

**This is the highest-severity failure in the entire pipeline.** A retrieval system that returns a document the user cannot read has caused a data breach, and the model will happily quote it.

Filtering must happen at or before search, not after:

```text
Wrong: search top-50 -> filter by ACL -> 3 survive -> answer from 3 (or from none)
Right: search with ACL predicate pushed into the index -> top-5 all authorized
```

Post-filtering is both a correctness bug and a quality bug: it silently shrinks the result set, and a user with narrow permissions gets a worse answer for reasons no log explains.

Store permissions as indexed metadata beside each vector and push the predicate into the query. Then test it adversarially: index a document only user A may see, query as user B, and assert it never appears - in the results, in the prompt, or in the answer. That test belongs in CI.

Metadata also carries the freshness and provenance fields you need: source id, version, timestamp, document type, tenant.

### Hybrid retrieval

Dense embeddings capture meaning but are weak exactly where precision matters: identifiers, error codes, product names, rare terms. Ask for `ERR_5521` and semantic search returns passages about errors generally. BM25 finds the exact token immediately.

Conversely, BM25 fails on "how do I stop it charging me twice" against a document titled "Preventing duplicate billing" - no shared terms, same meaning.

Run both and fuse. **Reciprocal rank fusion** avoids the problem that the two scoring systems are not on a comparable scale:

```text
score(d) = sum over retrievers of 1 / (k + rank(d))     with k ~ 60
```

RRF needs no score normalization and no tuning, which is why it is the sensible default. Hybrid retrieval is one of the few changes that reliably improves quality across almost every corpus, and it should be the first thing you propose when asked how to improve a RAG system.

### Reranking

Retrieval optimizes for recall - get the right chunk into the top 50. Reranking optimizes for precision - get it into the top 3.

A bi-encoder (the embedding model) encodes query and document separately, which is what makes indexing possible. A cross-encoder reads both together and scores relevance directly. It is far more accurate and far too slow to run over a whole corpus, so it runs over a shortlist:

```text
retrieve top 50 (fast, ~20ms) -> rerank to top 5 (slow, ~80ms) -> generate
```

The trade-off is explicit: roughly 80ms of TTFT for a substantial precision gain. Whether that is worth it depends on the latency budget from chapter 1. Name the trade-off rather than adopting the pattern silently.

### Assembling the prompt and forcing citations

Ordering matters. Models attend unevenly across a long context, with the middle receiving least attention - so put the highest-ranked chunks at the beginning and end, not buried in the middle.

Citations must be structural, not requested politely. Give each chunk an identifier and require the model to reference it:

```text
[doc_17] Free tier accounts are limited to 500 requests per hour.
[doc_23] Rate limits reset at the top of each hour, UTC.

Answer using only the passages above. Cite each claim as [doc_N].
If the passages do not contain the answer, say so.
```

Then **validate the citations in code**. Parse the identifiers out of the response and confirm each one was actually in the context you sent. A model that cites `[doc_31]` when you supplied nine chunks has hallucinated its own evidence, and that is programmatically detectable. Very few systems check this, and it is one of the cheapest quality controls available.

The "say so if the answer is absent" instruction matters too, and it works far better when retrieval scores are low enough that you can decline to call the model at all.

### Freshness and re-indexing

Two separate problems, often conflated:

**Incremental updates.** Documents change. Version chunks by source document, delete and re-embed changed ones, and reconcile periodically to catch missed deletions. A deleted document whose vectors remain will be cited confidently forever.

**Embedding model migration.** Vectors from different model versions are not comparable. Changing the model means re-embedding the entire corpus. Plan it as a migration: build the new index alongside, run both, compare recall on a labelled set, then cut over. Doing it in place produces a period where the index contains two incompatible vector spaces and quality collapses for reasons that are very hard to diagnose.

## Diagnosing "the answer was wrong"

This is the most valuable practical skill, and a common interview follow-up. The failure is at one of five stages, and each has a different fix:

```text
1. Not ingested        -> is the document in the corpus at all?
2. Badly chunked       -> is the fact split across a boundary, or diluted?
3. Not retrieved       -> is the chunk in the top-k? (recall@k)
4. Retrieved, not used -> was it in the prompt but ignored or buried mid-context?
5. Used, misread       -> was it in the prompt and misinterpreted?
```

Work the stages in order. Only stage 5 is a model problem, and it is the rarest. Most "the AI hallucinated" reports are stage 1 or stage 3, and the fix is ingestion or retrieval - not a better prompt and not a bigger model.

Instrument for this: log the retrieved chunk ids, their scores, and the assembled prompt for every request. Without that, every quality report is unfalsifiable.

## Failure modes and common mistakes

- Filtering by ACL after search instead of pushing the predicate into the index.
- No adversarial authorization test, so a permission bug ships silently.
- Chunks too small to be self-contained, or too large to embed distinctly.
- Embedding chunks without document and section context.
- Assuming ANN recall is perfect and never measuring recall@k.
- Reaching for a distributed vector database before computing the actual index size.
- Semantic-only retrieval, failing on identifiers, error codes, and product names.
- Changing the embedding model without re-indexing, mixing incompatible vector spaces.
- Deleting a source document without deleting its vectors.
- Burying the best chunk in the middle of a long context.
- Asking for citations without validating that cited ids were actually supplied.
- Calling the model even when top retrieval scores are far below threshold.
- No logging of retrieved chunk ids, making quality reports impossible to diagnose.
- Treating every wrong answer as a model problem when the cause is upstream.

## Interview questions and model answers

**Walk me through a RAG system.**

Two pipelines. Offline: extract, chunk with structural boundaries and overlap, embed, index with metadata including access control. Online: embed the query, hybrid search combining BM25 and vector with the ACL predicate pushed into the query, rerank the shortlist with a cross-encoder, assemble a prompt with the strongest chunks at the edges, generate with enforced citations, then validate those citations in code. The whole design assumes the model can only be as good as the retrieval.

**How do you enforce access control?**

Store permissions as indexed metadata beside each vector and push the predicate into the search query. Filtering after search is both a security risk and a quality bug, because it silently shrinks the result set. Then test adversarially in CI: index a document one user can see, query as another, assert it never reaches the results or the prompt.

**A user says the answer was wrong. How do you debug it?**

Five stages in order: was it ingested, was it chunked sensibly, was it retrieved into the top-k, was it in the prompt but ignored, was it in the prompt and misread. Only the last is a model problem and it is the rarest. This requires logging retrieved chunk ids and scores per request; without that the report cannot be investigated.

**Why combine keyword and vector search?**

They fail in opposite directions. Vector search misses exact identifiers, error codes, and rare terms. Keyword search misses paraphrase. Reciprocal rank fusion merges the two ranked lists without needing comparable scores. It is the single most reliable quality improvement across corpora.

**What happens when you change the embedding model?**

Every stored vector becomes incomparable, so the entire corpus must be re-embedded. Treat it as a schema migration: build the new index in parallel, compare recall@k on a labelled query set, then cut over. Doing it in place leaves two incompatible vector spaces in one index and degrades quality in a way that is very hard to diagnose.

**How do you keep the model from making things up?**

Retrieval quality first, since most fabrication is a missing-context problem. Then instruct it to answer only from the passages and to decline when they are insufficient. Then enforce citations and validate in code that every cited id was actually supplied. And set a retrieval score threshold below which you decline to call the model at all - the cheapest way to avoid a confident wrong answer is not to ask.

## Exercises

1. Design the chunking strategy for a corpus of API reference docs, runbooks, and Slack threads. Justify a different approach per source type.
2. Compute the index size for 2 million chunks at 1,536 dimensions, then decide between in-memory exact search, HNSW, and a hosted vector database.
3. Write the adversarial access-control test described above, and state where it belongs in CI.
4. Implement reciprocal rank fusion over two ranked lists and show a query where it beats either retriever alone.
5. Given a wrong answer and the logged chunk ids and scores, work the five-stage diagnosis and name the fix at each stage.
6. Plan an embedding model migration for a live system with a zero-quality-regression requirement. Specify the comparison metric and the rollback trigger.
7. Design the citation validator: parse ids from the response, check them against the supplied context, and decide what to do on a mismatch.

## Chapter summary

RAG is a distributed data problem before it is an AI problem, and the model can only be as good as what retrieval hands it. Chunking constrains everything downstream: aim for self-contained units of 400 to 800 tokens, split on real structure, prefix each chunk with its document and section context, and consider retrieving small while passing the parent section. Access control must be pushed into the search query rather than applied afterwards, and it must be tested adversarially, because a retrieval leak is a data breach that the model will read aloud. Hybrid retrieval fused with RRF is the most dependable quality win available, and reranking buys precision for a named latency cost. Enforce citations structurally and validate them in code. When an answer is wrong, walk the five stages in order - most failures are ingestion or retrieval, and almost none are the model.

## Revision checklist

- [ ] I can draw both the ingestion and query pipelines from memory.
- [ ] I can pick a chunking strategy per source type and justify the size.
- [ ] I know why contextual headers on chunks improve retrieval so much.
- [ ] I can explain HNSW versus IVF and which parameter trades recall for latency.
- [ ] I always push ACL predicates into the search rather than filtering after.
- [ ] I can write the adversarial authorization test.
- [ ] I can explain why hybrid retrieval beats either method and how RRF merges them.
- [ ] I can state the latency cost of reranking and decide whether to pay it.
- [ ] I validate returned citations against the supplied context in code.
- [ ] I can run the five-stage wrong-answer diagnosis and name the fix at each stage.
- [ ] I treat an embedding model change as a full re-index migration.
