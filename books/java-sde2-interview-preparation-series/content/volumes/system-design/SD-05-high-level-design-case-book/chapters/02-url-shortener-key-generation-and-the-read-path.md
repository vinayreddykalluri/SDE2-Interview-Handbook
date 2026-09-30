# 2. URL Shortener: Key Generation and the Read Path

## Learning objectives

By the end of this chapter, you should be able to:

- estimate the load and storage of a URL shortener and draw the conclusion the numbers support;
- compare random, hashed, and counter-based key generation with actual collision arithmetic;
- design a key-generation service that needs no coordination on the write path;
- choose between 301 and 302 redirects and explain what each costs; and
- size the cache on the read path and say what happens when it fails.

## Why this matters at SDE-2

The URL shortener is the most common warm-up prompt, which makes it a strong filter: most candidates produce the same diagram, and the difference between a pass and a strong pass is entirely in the reasoning. Specifically, it is in key generation. "Hash the URL and take the first seven characters" is the answer most candidates give, and at the stated scale it produces millions of collisions. A candidate who computes that - and designs around it - has shown the core SDE-2 skill of letting numbers drive the design.

## First-principles model

A URL shortener is **a globally unique key allocator in front of a read-heavy key-value lookup.**

- The **write path** allocates a short, unique key and stores `key -> long URL`.
- The **read path** looks the key up and redirects.
- The **invariant** is that a key, once issued, always points to the same URL. Handing out a key twice silently redirects someone's link to a stranger's URL - a correctness and security failure, not a performance one.

## Core terminology

- **Short code:** the key in the short URL, such as `2crtgcg`.
- **Base62:** encoding with digits, lowercase, and uppercase letters - 62 symbols, URL-safe without escaping.
- **Key space:** the number of distinct codes of a given length; `62^7` for seven characters.
- **Birthday bound:** the collision count for random keys, approximately `n^2 / (2N)` for `n` keys drawn from a space of `N`.
- **Key range allocation:** handing each application server a block of counter values to use without further coordination.
- **301 / 302:** permanent and temporary HTTP redirects.

## Detailed mechanics

### Requirements and estimate

In scope: create a short link for a long URL; redirect a short link; links do not expire by default. Out of scope: custom aliases (mentioned as an extension), analytics dashboards, and user accounts.

Non-functional: redirects are the hot path and need low latency; a key must never point to the wrong URL; created links must survive the loss of any single machine.

The companion computes the envelope:

```text
writes/s 38.6 average, reads/s 3,858 average
5 years -> 6,000,000,000 records, 3.0 TB at 500 bytes each
```

The conclusion to say out loud: **this is a small-data, read-heavy system**. Three terabytes fits on one machine; throughput is modest. The design problems are key generation, availability, and read latency - not storage scale.

### Key generation: three options, with arithmetic

**Option 1: random or hashed keys, truncated to seven characters.** Seven base62 characters give `62^7 = 3.52 x 10^12` codes, which sounds inexhaustible. But collisions follow the birthday bound, not the key-space size:

```text
random 7-char keys for 6,000,000,000 URLs: ~5,111,292 expected collisions
```

Five million collisions over five years, and every one must be detected (a check before insert) and retried. Hashing the long URL instead of drawing randomly has the same collision behaviour for different URLs, with one advantage - identical URLs map to the same key - that is also a disadvantage if two users should get separate links for analytics.

The birthday formula is worth trusting, and the companion checks it at a scale where it can be simulated:

```text
birthday check, 2,000 keys into 62^3: predicted 8.39 colliding pairs, measured 8.41 over 400 trials
```

**Option 2: a single counter, encoded in base62.** No collisions by construction, and short codes:

```text
counter id 125,000,000,000 -> base62 "2crtgcg" (7 chars)
62^7 = 3.522e+12 keys; a counter lasts 2,894 years at this rate
```

The problem is that a single counter is a single point of failure and a coordination point on every write.

**Option 3: counter ranges.** A small coordination service - a database row updated transactionally, or a consensus-backed store such as ZooKeeper or etcd - hands each application server a block of, say, 10,000 ids. The server allocates from its block in memory with no network call and fetches a new block when it runs low. At 38.6 writes per second, a block of 10,000 lasts over four minutes on a single server, so the coordination service sees a request every few minutes.

Trade-offs to state:

- A server that crashes loses the rest of its block. At 3.5 trillion codes that waste is irrelevant; say so rather than engineering around it.
- Sequential codes are guessable. If links must not be enumerable, either encrypt the counter with a keyed 64-bit permutation before encoding - still collision-free, because a permutation is one-to-one - or add a random suffix and accept a longer code.

Option 3 is the answer to lead with: no collisions, no per-write coordination, and a failure mode that costs only unused ids.

### Data model and storage

```text
links:  code (PK, string)  |  long_url (string)  |  created_at  |  owner_id (nullable)
```

Every read is a point lookup by primary key, and no query needs a join. A key-value or wide-column store fits; a relational database also works comfortably at this size. Choose by operational familiarity and say that the access pattern does not force the choice. Replicate across at least three nodes in separate failure domains for durability.

### The read path

```text
client -> CDN / edge -> redirect service -> cache -> links store
```

At 3,858 average reads per second - perhaps 20,000 at peak - with a skewed popularity distribution, a cache in front of the store absorbs most reads. A modest cache holding the hottest few percent of links typically achieves a high hit rate on a Zipf-like workload; state the assumption and size it from the measured hit rate rather than presenting a guessed number as fact.

What happens when the cache is lost? The store sees the full read rate. At these numbers a replicated store can serve that directly, so the failure raises latency rather than causing an outage. Say this: it is the difference between a cache for latency and a cache that the system depends on for survival.

### 301 or 302?

- **301 (permanent):** browsers and proxies may cache the redirect, so repeat visits never reach you. Lower load and lower latency, but you lose visibility into clicks and cannot change the destination.
- **302 (temporary):** every visit reaches the service. Higher load, full analytics, and the destination can be changed or the link disabled - which matters for abuse takedowns.

Most production shorteners use a temporary redirect for control and analytics. State the trade-off and choose.

### Analytics without slowing the redirect

Clicks are recorded by emitting an event to a queue after the redirect response is sent, never by a synchronous database write on the hot path. A stream processor aggregates counts per link per time bucket. If the queue is down, the redirect still works; analytics are delayed or sampled.

## Failure modes and common mistakes

- **Truncated hash without collision handling.** Millions of collisions at the stated scale.
- **Single global counter.** A coordination point and single point of failure on every write.
- **Check-then-insert race.** Two servers generating the same random key both check, both see it free, both insert. Use a unique constraint and retry on violation.
- **Synchronous analytics.** Click logging on the redirect path turns an analytics outage into a redirect outage.
- **301 without saying what it costs.** Losing takedown ability is a real product risk.

## Interview questions and model answers

**Q: How do you generate the short code?**
A: A base62-encoded counter, with ranges of ids handed to each server by a small coordination service so there is no per-write coordination. It cannot collide. Truncating a hash to seven characters looks simpler, but with six billion links the birthday bound gives about five million collisions, each needing detection and retry.

**Q: Isn't a sequential code guessable?**
A: Yes. If enumeration matters, I apply a keyed 64-bit permutation to the counter before encoding. A permutation is one-to-one, so it stays collision-free while the codes stop being sequential.

**Q: What happens if the cache goes down?**
A: The store sees the full read rate - around 4,000 per second on average. A replicated store can serve that, so latency rises but redirects keep working. I'd alert on cache hit rate and store latency.

**Q: 301 or 302?**
A: 302 by default. A 301 lets browsers cache the redirect, which saves load but gives up click analytics and the ability to disable a malicious link. If load became the problem, I'd put a CDN in front before switching to 301.

## Exercises

1. Recompute the estimate for 1 billion new links per month. What changes in the design, and what does not?
2. How many base62 characters does a random-key scheme need to keep expected collisions under one over five years at 100 million links per month?
3. Design custom aliases. How do they interact with counter-generated codes, and how do you stop a custom alias from later colliding with a generated one?

## Chapter summary

A URL shortener is a unique-key allocator in front of a read-heavy lookup. The estimate shows a small-data, read-heavy system, so the difficulty is key generation. Truncated hashes collide millions of times at the stated scale; counter ranges encoded in base62 cannot collide and need no per-write coordination. Cache the read path for latency, keep analytics off it, and choose the redirect status deliberately.

## Revision checklist

- [ ] I can produce the estimate and state what it implies.
- [ ] I can apply the birthday bound and explain why seven characters is not enough for random keys at this scale.
- [ ] I can design counter-range allocation and describe its failure mode.
- [ ] I can make codes non-enumerable without reintroducing collisions.
- [ ] I can defend 302 over 301.
