# 4. News Feed: Fan-Out and the Celebrity Problem

## Learning objectives

By the end of this chapter, you should be able to:

- compare fan-out on write (push) and fan-out on read (pull) by the work each does per post and per feed read;
- quantify how a skewed follower graph concentrates fan-out cost in a tiny fraction of accounts;
- design the hybrid model and choose its threshold with arithmetic rather than instinct;
- design the timeline store, the ranking step, and pagination; and
- handle deletes, privacy changes, and newly followed accounts.

## Why this matters at SDE-2

"Design Twitter" or "design the Instagram feed" is where interviewers test whether a candidate can reason about *skew*. The averages are easy and misleading. The design is decided by the tail: a handful of accounts with tens of millions of followers. The strongest answers put a number on that tail and let it pick the architecture.

## First-principles model

A feed is **a per-user, time-ordered merge of posts from the accounts that user follows.** The only real question is *when* the merge happens:

- **Push (fan-out on write):** when an account posts, write the post id into the precomputed timeline of every follower. Reads are cheap - one lookup. Writes cost one insert per follower.
- **Pull (fan-out on read):** store posts only in the author's list. When a user opens the feed, fetch recent posts from every followed account and merge. Writes are cheap; reads cost one fetch per followed account.

Neither is right in general. The follower graph decides.

## Core terminology

- **Timeline (home feed):** the list of post ids a user sees, newest or highest-ranked first.
- **Fan-out:** copying one post id into many timelines.
- **Write amplification:** timeline writes per post.
- **Zipf distribution:** a power law where the k-th most popular item has popularity proportional to `1/k`.
- **Hybrid fan-out:** push for ordinary accounts, pull for accounts above a follower threshold.
- **Cursor pagination:** continuing a feed from an opaque position instead of a page number.

## Detailed mechanics

### Requirements and the naive estimate

In scope: post, follow, read a home feed of recent posts from followed accounts, paginate. Out of scope: search, trends, direct messages. Non-functional: feed reads are the hot path and should be fast; a new post should appear in followers' feeds within seconds, not instantly; losing a post is not acceptable.

The companion builds a follower graph for 1,000,000 accounts where the account at popularity rank `r` has `10,000,000 / r` followers - a Zipf shape, which is what real social graphs roughly look like at the top. If every account posts once a day and we push:

```text
one post per account per day, push model: 143,435,683 timeline writes/day
that is 1,660 writes/s on average
```

On average that is easy. The average is not the problem.

### The tail decides

The same model, splitting accounts by follower count:

```text
pull for accounts over 100,000 followers:     99 accounts (0.010%) remove 36% of fan-out writes
pull for accounts over  10,000 followers:    999 accounts (0.100%) remove 52% of fan-out writes
pull for accounts over   1,000 followers:  9,990 accounts (0.999%) remove 68% of fan-out writes
```

One account in a thousand is responsible for more than half of all fan-out writes. And the cost is not only volume but *latency*:

```text
top account's single post at 50,000 writes/s: 200 s to fan out
```

A single post from the top account needs ten million timeline inserts. Even at 50,000 inserts per second dedicated to it, the last follower sees it more than three minutes later, and meanwhile that work queues in front of every ordinary account's posts.

### The hybrid model

Push for accounts below a follower threshold; pull for accounts above it.

- **On post:** if the author is below the threshold, enqueue a fan-out job that inserts the post id into each follower's timeline. Otherwise, write the post only to the author's own post list.
- **On feed read:** fetch the user's precomputed timeline, fetch recent posts from the few above-threshold accounts they follow, merge by time or score, and return a page.

The read cost of the pull side is bounded by how many celebrity accounts a user follows, which is small for almost everyone. The threshold is a tuning knob: lower it and you save more writes but add more merges per read. With the numbers above, a threshold of 10,000 followers pulls for 999 accounts and removes over half the write load - a good default to propose, with the arithmetic that justifies it.

### The timeline store

A timeline is a bounded list of post ids per user - say the latest 800 - stored in a fast in-memory store keyed by user id. Store **ids, not post bodies**: a post edited or deleted later is then correct everywhere with one update, and timelines stay small. Hydrate ids into full posts from a post cache at read time.

Two refinements to mention:

- **Inactive users:** skip fan-out for users who have not opened the app in, say, 30 days, and rebuild their timeline by pulling when they return. This removes a large share of writes for free.
- **Bounded length:** trim timelines on write. Older history is served by pulling from post lists, which is rare.

### Ranking

Chronological feeds merge by time. Ranked feeds fetch a few hundred candidates - from the timeline and the pulled celebrity posts - and score them with a model, then return the top page. Keep ranking in a separate service with its own latency budget and a fallback to chronological order if it times out. The feed must not fail because ranking did.

### Pagination

Use cursor pagination: the response includes an opaque cursor encoding the last post's score or timestamp and id, and the next request continues from it. Page numbers break when new posts arrive between requests; cursors do not.

### Deletes, privacy, and new follows

- **Delete:** mark the post deleted in the post store; hydration drops it. Optionally clean timelines lazily.
- **Account goes private or blocks a follower:** filter at hydration time against the current relationship, because timelines may still hold old ids.
- **New follow:** backfill the new account's recent posts into the follower's timeline asynchronously, or rely on the pull path until the next post.

## Failure modes and common mistakes

- **Choosing push or pull without the follower distribution.** The tail, not the average, decides.
- **Fan-out as one synchronous loop.** Must be queued, batched, and retryable.
- **Storing post bodies in timelines.** Edits and deletes become a fan-out problem too.
- **Ranking on the critical path without a fallback.** A ranking outage becomes a feed outage.
- **Offset pagination** on a feed that changes while the user scrolls.

## Interview questions and model answers

**Q: Push or pull?**
A: Hybrid, decided by the follower distribution. In a Zipf-shaped graph of a million accounts, the 999 accounts with more than 10,000 followers cause 52% of all fan-out writes, and the top account's single post needs ten million inserts - over three minutes at 50,000 writes per second. So I push for ordinary accounts and pull for accounts above a threshold, merging the two on read.

**Q: How does the read path work in the hybrid model?**
A: Fetch the user's precomputed timeline of post ids, fetch recent posts from the few above-threshold accounts they follow, merge by time or score, hydrate the ids into posts from a cache, and return a page with a cursor.

**Q: A celebrity deletes a post. What changes?**
A: Only the post store: it is marked deleted and hydration drops it. Timelines hold ids, so nothing needs to be rewritten, and celebrity posts were never fanned out anyway.

**Q: How do you avoid wasting writes on users who never open the app?**
A: Skip fan-out for users inactive beyond a window and rebuild their timeline by pulling when they return.

## Exercises

1. Recompute the fan-out table for a graph where the top account has 100 million followers. Does the threshold change?
2. Estimate the read-side cost of the hybrid model for a user who follows 50 above-threshold accounts. How would you cap it?
3. Design the fan-out worker: batching, retries, and what happens if it crashes halfway through a ten-million-follower post.

## Chapter summary

A feed is a per-user merge of followed accounts' posts; push does the merge on write, pull on read. Follower graphs are heavily skewed - in the model, 0.1% of accounts cause over half of fan-out writes - so the answer is hybrid: push for most accounts, pull for the tail, merge on read. Store ids rather than posts, skip inactive users, keep ranking off the critical path, and paginate with cursors.

## Revision checklist

- [ ] I can state the per-post and per-read cost of push and of pull.
- [ ] I can quantify how skew concentrates fan-out cost, with the 0.1% / 52% example.
- [ ] I can describe the hybrid read path end to end.
- [ ] I can explain why timelines store ids, and how deletes and privacy changes are handled.
