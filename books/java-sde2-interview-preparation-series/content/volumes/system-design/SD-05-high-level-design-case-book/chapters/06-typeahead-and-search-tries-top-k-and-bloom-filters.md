# 6. Typeahead and Search: Tries, Top-K, and Bloom Filters

## Learning objectives

By the end of this chapter, you should be able to:

- design a typeahead service whose query path does no ranking work at request time;
- separate the online serving path from the offline aggregation path, and state the freshness trade-off;
- partition a prefix index without creating hot shards;
- size a Bloom filter for a target false-positive rate and explain where one saves real work; and
- sketch an inverted index for full-text search and say what makes it hard to update.

## Why this matters at SDE-2

Typeahead is a latency problem disguised as a data-structure problem. A candidate who builds a trie and walks it on every keystroke has built something correct and too slow. The SDE-2 move is to precompute: the answer for every popular prefix exists before anyone types it, and the serving path is a lookup. Search and typeahead also share a component that interviewers like to probe - the probabilistic filter that avoids expensive lookups for things that are not there.

## First-principles model

Typeahead is **a precomputed map from prefix to its top-k completions, rebuilt periodically from query logs, and served from memory.**

- **Online path:** keystroke -> prefix -> lookup -> k suggestions. Budget: tens of milliseconds end to end, because it runs on every keystroke.
- **Offline path:** query log -> aggregate counts over a window -> compute top-k per prefix -> publish a new index.

Nothing on the online path counts, sorts, or ranks. All of that happened offline.

## Core terminology

- **Trie (prefix tree):** a tree whose paths spell strings; every node represents a prefix.
- **Top-k per node:** the k highest-scoring completions stored at each prefix node.
- **Inverted index:** a map from each term to the list of documents containing it.
- **Bloom filter:** a bit array with `k` hash functions that answers "definitely not present" or "possibly present".
- **False positive rate:** the fraction of absent keys a Bloom filter reports as possibly present.
- **Freshness:** how long a new popular query takes to appear in suggestions.

## Detailed mechanics

### Requirements and estimate

In scope: suggest up to 10 completions for a typed prefix, ranked by popularity, with some personalisation mentioned as an extension. Out of scope: spelling correction beyond a mention, and the search results page itself.

Non-functional: p99 well under 100 ms end to end; suggestions may lag real query trends by minutes to an hour.

A useful framing for the estimate: with 100 million searches a day and about five keystrokes that trigger a suggestion request per search, the service sees about 500 million requests per day - roughly 5,800 per second on average and several times that at peak. The index itself is small: the set of distinct popular queries and their prefixes fits in memory on each serving node.

### Precompute top-k at every node

Store the top 10 completions at every trie node, so a lookup is a walk of at most the prefix length, then a read of a stored list. In practice the trie is flattened into a key-value map from prefix string to its list, which is simpler to shard, replicate, and serve.

Limit which prefixes get entries: very short prefixes are served from a small, heavily cached table, and prefixes with too little traffic are not stored at all.

### The offline path and freshness

1. Query logs stream into a log store.
2. A batch or streaming job counts queries over a sliding window, with decay so that yesterday's spike fades.
3. The job computes the top-k per prefix and writes a new index version.
4. Serving nodes load the new version and switch atomically.

Freshness is a product decision. A job every 15 minutes means a breaking-news query takes up to 15 minutes, plus build time, to appear. If that is too slow, add a small real-time layer that tracks trending queries over the last few minutes and merges them into the stored suggestions at serving time.

Filtering belongs in this path too: remove offensive, private, or legally blocked queries before they reach the index, not at serving time.

### Partitioning the prefix index

Partitioning by first letter is the obvious answer and a bad one: the traffic for "s" is many times the traffic for "x". Better options:

- **Replicate the whole index** on every serving node if it fits in memory. This is often the right answer, and it removes the partitioning question entirely.
- **Partition by hash of the prefix** when it does not fit. Balanced, and each request touches one shard, because the full prefix is known.

### Bloom filters: saving expensive lookups

A Bloom filter answers set-membership with no false negatives and a tunable false-positive rate, using a fraction of the memory of storing the keys. The standard sizing for `n` items and target rate `p`:

```text
m = -n ln(p) / (ln 2)^2      bits
k = (m / n) ln 2             hash functions
```

The companion sizes one for 100,000 keys at 1%, inserts them, and measures:

```text
m = 958,506 bits (117 KB), k = 7 hashes, predicted FP 1.004%
measured on 200,000 non-members: 1.013%; 0 false negatives on 100,000 members
```

The measurement matches the formula, and there are no false negatives, which is the property that makes the structure useful. It uses about 9.6 bits per key regardless of how long the keys are.

Where it saves work in these systems:

- **LSM-tree storage** (Cassandra, RocksDB, HBase) keeps one filter per on-disk file, so a read for an absent key skips files without touching disk.
- **Web crawlers** check "have we already queued this URL?" without a database round trip.
- **Search and feed systems** avoid a remote lookup for items known to be absent, such as "has this user already seen this post?"

Two limitations to state: a standard Bloom filter cannot delete (a counting Bloom filter or a cuckoo filter can), and the false-positive rate climbs quickly once you insert more than the `n` it was sized for.

### Full-text search in one paragraph

Search results use an inverted index: for each term, the list of document ids that contain it, sorted, with positions for phrase queries. A query intersects the lists for its terms and ranks the matches. Updates are the hard part - rewriting long posting lists on every change is too expensive - so search engines append new documents into small new segments and merge segments in the background, which is the same log-structured idea as an LSM tree. Near-real-time search means a new document is searchable once its segment is opened, typically within a second.

## Failure modes and common mistakes

- **Walking and ranking on every keystroke.** Precompute top-k per prefix.
- **Partitioning by first letter.** Traffic per letter is extremely uneven.
- **No freshness story.** Say how long a new trend takes to appear, and how to shorten it if needed.
- **Filtering at serving time only.** Offensive or private queries should never enter the index.
- **Bloom filter without sizing.** Overfill it and the false-positive rate quietly climbs.
- **Expecting deletes from a standard Bloom filter.** Use a counting or cuckoo filter.

## Interview questions and model answers

**Q: How do you make typeahead fast?**
A: Precompute the top 10 completions for each prefix offline from the query logs, and serve lookups from an in-memory map on each node. Nothing is counted or sorted at request time. If the index fits in memory, I replicate it to every node rather than partitioning.

**Q: How fresh are the suggestions?**
A: As fresh as the rebuild interval plus build time - say 15 to 30 minutes with a 15-minute job. If trending queries need to appear faster, I add a small streaming layer that tracks the last few minutes and merges its results in at serving time.

**Q: Where would you use a Bloom filter, and how big is it?**
A: In front of any lookup that is expensive when the key is absent - LSM-tree files, crawler URL de-duplication, "already seen" checks. For 100,000 keys at 1%, it is about 958,000 bits - 117 KB - with 7 hash functions; I measured 1.01% false positives and no false negatives.

**Q: How does a search index handle updates?**
A: New documents go into small new segments, which are merged in the background. Posting lists are never rewritten in place, which keeps writes cheap and makes new documents searchable within about a second.

## Exercises

1. Size a Bloom filter for 1 billion URLs at 0.1% false positives. How much memory, and how many hash functions?
2. Add personalisation to typeahead - the user's own recent searches - without breaking the no-ranking-at-request-time rule.
3. Estimate the size of a prefix index for 10 million distinct popular queries averaging 20 characters, storing 10 completions per prefix.

## Chapter summary

Typeahead precomputes the top-k completions for every popular prefix offline and serves them from memory; freshness is the rebuild interval, and a small streaming layer can shorten it. Replicate the index if it fits, and hash-partition prefixes if it does not. Bloom filters answer "definitely not present" at about 9.6 bits per key for 1% false positives, which is why storage engines and crawlers use them to avoid expensive lookups.

## Revision checklist

- [ ] I can describe the online and offline paths of typeahead separately.
- [ ] I can explain why first-letter partitioning fails.
- [ ] I can size a Bloom filter from `n` and `p`, and name three places it saves work.
- [ ] I can explain how an inverted index takes updates without rewriting posting lists.
