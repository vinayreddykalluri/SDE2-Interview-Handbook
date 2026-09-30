# 10. Case Bank, Follow-Ups, and Scoring

## Learning objectives

By the end of this chapter, you should be able to:

- map an unfamiliar prompt onto the cases in this book by its dominant difficulty;
- answer the follow-up questions that close most design rounds;
- score your own mock rounds against a consistent rubric; and
- choose which case to practise next from your scores.

## Why this matters at SDE-2

No list of prompts covers every interview. What transfers is recognition: most new prompts are one of a handful of difficulties in a new costume. "Design a leaderboard" is the cache and hot-key problem from chapter 3 with a sorted set. "Design Google Docs" is chat's ordering problem from chapter 5 with operational transforms or CRDTs. A candidate who recognises the difficulty in the first five minutes spends the rest of the round on the right thing.

## First-principles model

Every prompt in this book is dominated by one of six difficulties:

| Difficulty | Cases in this book | The question it forces |
|---|---|---|
| Unique allocation | URL shortener, seat booking | how to hand out ids or inventory without collisions or double-selling |
| Partitioning and skew | cache and KV store, feed | how to spread load when keys or users are extremely uneven |
| Ordering and delivery | chat, payments | how to get exactly-once effects in order over an unreliable network |
| Precomputation and freshness | typeahead, feed ranking | what to compute ahead of time, and how stale it may be |
| Spatial and moving data | ride matching | how to index points that are near each other and keep moving |
| Volume and durability | metrics, object storage | how many bytes, for how long, and how they survive failures |

Identify the difficulty, then reuse the reasoning from the matching chapter.

## Core terminology

- **Dominant difficulty:** the requirement that is hardest to meet at the estimated load.
- **Follow-up:** a question after the main design about failure, scale, or change.
- **Rubric:** the dimensions an interviewer scores, stated so that you can score yourself.

## Detailed mechanics

### Mapping new prompts

| New prompt | Closest case | What changes |
|---|---|---|
| Pastebin | URL shortener | objects are larger: store bodies in an object store, keep metadata in the key-value store |
| Leaderboard | Cache and KV store | sorted sets per board; the top board is a hot key |
| Collaborative editor | Chat | ordering per document; concurrent edits need operational transforms or CRDTs |
| Notification system | Chat and feed | fan-out to devices; preferences and rate limits per user |
| Web crawler | Typeahead (Bloom filter) and metrics (volume) | URL frontier, politeness per host, de-duplication with a Bloom filter |
| Distributed job scheduler | Payments (exactly-once) | leases and idempotent job execution |
| Video streaming | Object storage | chunked storage, CDN, adaptive bitrate |
| Nearby friends | Ride matching | both parties move; privacy controls who is visible |
| Stock exchange order book | Payments and booking | strict ordering per instrument on a single writer |

### The follow-ups that close the round

Prepare a crisp answer for each of these against every case you practise.

1. **"What happens at 10x load?"** Name the first component to saturate, the metric that would show it, and the change you would make.
2. **"What if a data centre is lost?"** State what is replicated across regions, what the recovery point and recovery time are, and which features degrade.
3. **"How do you deploy a schema change?"** Expand-and-contract: add the new field, dual-write, backfill, switch reads, then remove the old field.
4. **"What do you monitor?"** The SLIs for each user-facing flow, plus the saturation signal of the component you expect to fail first.
5. **"Where is the single point of failure?"** There usually is one - a coordinator, a registry, a primary database. Name it and its failover.
6. **"How would you test this?"** Load tests at the estimated peak, fault injection for the failure cases you described, and reconciliation for anything involving money or inventory.

### Scoring a mock round

Score each mock from 0 to 2 on six dimensions:

```text
                          0                        1                          2
requirements        none written             functional only            functional, non-functional, non-goals
estimation          no numbers               numbers, not used          numbers that chose the design
high-level design   unlabelled boxes         flows drawn                every arrow and store labelled
deep dive           easy component           right component, shallow   right component, alternatives, failure modes
trade-offs          none stated              stated when asked          volunteered, with a decision
failure and scale   not discussed            mentioned                  failure behaviour and first bottleneck named
```

Nine or more out of twelve is a strong SDE-2 round. Keep the sheets: the dimension that stays lowest across attempts is what to practise next, and for most candidates it is estimation or failure behaviour rather than architecture.

## Case bank

Practise each at 45 minutes, and score it.

**Unique allocation:** URL shortener with custom aliases; concert ticketing with seat maps; coupon code issuance with per-user limits.

**Partitioning and skew:** distributed cache with hot-key handling; leaderboard for 100 million players; key-value store with tunable consistency.

**Ordering and delivery:** one-to-one and group chat; collaborative document editing; payment system with refunds.

**Precomputation and freshness:** typeahead; home feed with ranking; trending topics over the last hour.

**Spatial and moving data:** ride matching; restaurant search by distance; delivery-driver dispatch.

**Volume and durability:** metrics and alerting; object storage; log aggregation and search.

## Failure modes and common mistakes

- **Treating every prompt as new.** Most are a known difficulty in a new costume.
- **Unprepared follow-ups.** The last ten minutes are predictable; prepare them.
- **Practising without scoring.** Repetition without a score strengthens what is already strong.

## Interview questions and model answers

**Q: You have never seen this prompt. How do you start?**
A: The same way as any other: requirements, non-goals and an estimate. Then I identify the dominant difficulty - allocation, skew, ordering, precomputation, spatial data, or volume - and reuse the reasoning I know for it.

**Q: What happens if a region goes down?**
A: I say what is replicated across regions and how - synchronously for money and inventory, asynchronously for most other data - which gives the recovery point, and how traffic fails over, which gives the recovery time. Then I name the features that degrade rather than fail.

**Q: How do you change a schema without downtime?**
A: Expand and contract: add the new column, write to both, backfill, switch reads to the new column, then stop writing and drop the old one. Each step is reversible until the last.

## Exercises

1. Pick three prompts from the mapping table that you have not practised. Identify the dominant difficulty of each in two minutes, then run a full 45-minute mock on one.
2. Write your prepared answer to each of the six follow-ups for the case you know best.
3. Score your last three mock rounds with the rubric. Which dimension is lowest?

## Chapter summary

New prompts are usually one of six known difficulties: unique allocation, skew, ordering, precomputation, spatial data, or volume. Recognise the difficulty early and reuse the matching reasoning. Prepare the six standard follow-ups, and score every mock on six dimensions so that practice targets your weakest one.

## Revision checklist

- [ ] I can name the dominant difficulty of each case in this book.
- [ ] I can map five unfamiliar prompts onto those difficulties.
- [ ] I have prepared answers to the six standard follow-ups.
- [ ] I score every mock round out of twelve.
