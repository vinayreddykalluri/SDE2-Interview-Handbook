# 1. The HLD Case Method and Estimation Toolkit

## Learning objectives

By the end of this chapter, you should be able to:

- run a 45-minute high-level design round with a fixed structure and a time budget;
- turn a vague prompt into functional requirements, non-functional requirements, and explicit non-goals;
- produce a capacity estimate in under five minutes, with rounding that is honest about its error;
- choose the one or two components that deserve a deep dive, and defend the choice; and
- recognise the difference between an SDE-1 and an SDE-2 answer to the same prompt.

## Why this matters at SDE-2

SD-02 taught the building blocks: partitioning, replication, consistency, queues, and failure handling. This book applies them to the prompts that interviewers actually use. Every case follows the same template, because the template is the skill. Candidates rarely fail a design round for not knowing what a message queue is. They fail because they draw twelve boxes in the first ten minutes, never state a number, and spend the deep dive on the component the interviewer cared least about.

At SDE-2 the bar moves in three specific ways. You are expected to **drive** the conversation rather than wait for questions; to **quantify** before choosing technology; and to **name the failure mode** of each component you add. An SDE-1 answer to "design a URL shortener" is a correct diagram. An SDE-2 answer is the same diagram plus "at 3,900 reads per second a single cache node is enough, the interesting problem is key generation, and here is why random 7-character keys will collide five million times over five years".

## First-principles model

A high-level design is **a set of data flows sized against a load, with a stated failure behaviour for each flow.**

- **Flows** come from the functional requirements: create a short link, follow a short link, post, read a feed.
- **Load** comes from the estimate: requests per second on each flow, bytes stored, bytes moved.
- **Failure behaviour** comes from the non-functional requirements: what may be stale, what may be lost, what must never be duplicated.

Everything on the whiteboard should trace back to one of those three. A box that serves no flow, handles no load, and changes no failure behaviour is decoration.

> **Specification boundary:** there is no standard answer to an HLD prompt, and interviewers know it. What they grade is whether your choices follow from your stated requirements and numbers. Two candidates can choose different databases and both pass; a candidate who cannot say *why* their database fits the access pattern will not.

## Core terminology

- **Functional requirement:** a behaviour the system must provide ("follow a short link").
- **Non-functional requirement:** a quality it must have ("p99 redirect under 50 ms", "no link ever points to the wrong URL").
- **Non-goal:** something explicitly out of scope, stated so that it is not silently assumed.
- **Back-of-the-envelope estimate:** a capacity calculation accurate to within a factor of about two.
- **Hot path:** the flow that carries most of the traffic or is most latency-sensitive.
- **Deep dive:** the part of the round where one component is designed in detail.
- **Read/write ratio:** the proportion of read to write traffic, which drives caching and replication decisions.

## Detailed mechanics

### The 45-minute structure

```text
minutes   step                          artifact on the board
0-5       requirements                  3-5 functional, 3-5 non-functional, 2-3 non-goals
5-10      estimate                      QPS per flow, storage, bandwidth, read/write ratio
10-15     API and data model            endpoint signatures; entities and keys
15-25     high-level design             boxes and arrows for each flow, labelled with QPS
25-40     deep dives (1-2)              the hard component, alternatives, failure modes
40-45     wrap-up                       bottlenecks, what breaks at 10x, what you would monitor
```

The allocation is deliberately generous to requirements and estimation. They take ten minutes and determine every later decision. Candidates who skip them spend the deep dive defending choices they cannot justify.

### Requirements: write them down

State requirements as a written list on the board, in the interviewer's words where possible. Then add the non-functional ones yourself - interviewers often leave them out on purpose:

- **Scale:** daily active users, requests per user per day, data size per item.
- **Latency:** which flow is user-facing and what p99 it needs.
- **Consistency:** which reads may be stale, and by how much.
- **Durability:** which data must never be lost.
- **Availability:** which flows must keep working during a partial outage.

Then state two or three **non-goals**. "Custom domains and link analytics dashboards are out of scope; I'll mention where analytics would attach." Non-goals protect your time and show judgement.

### The estimation toolkit

Estimation is arithmetic with honest rounding. Memorise a few anchors and derive everything else:

```text
seconds per day            86,400        ~ 1e5  (round up; errs toward over-provisioning)
seconds per 30-day month   2,592,000     ~ 2.6e6
1 million per day                        ~ 12 per second
1 billion per month                      ~ 386 per second
peak multiplier                          2x-10x average, depending on diurnal shape
```

Every case in this book shows its arithmetic, and the companion model recomputes it. Take the URL shortener in chapter 2:

```text
new links    100,000,000 / month  ->  38.6 writes/s average
reads        100 per write        ->  3,858 reads/s average
records      5 years              ->  6,000,000,000
storage      500 bytes each       ->  3.0 TB
```

Four numbers, two minutes. They already say that this is a read-heavy, small-data system: 3 TB fits on a single modern machine, so sharding is a question of throughput and availability, not of storage. That observation changes the rest of the design, and it is only available to a candidate who did the arithmetic.

**Rounding rule:** round in the direction that makes the design safer - up for load and storage - and say so. A factor-of-two error is fine; a factor-of-a-thousand error, such as confusing per-day with per-second, is disqualifying.

### Choosing the deep dive

Every prompt has one component where the difficulty lives. Find it by asking which requirement is hardest to meet at the estimated load:

| Prompt | Where the difficulty lives |
|---|---|
| URL shortener | key generation without collisions or coordination |
| Distributed cache | partitioning, rebalancing, hot keys |
| News feed | fan-out cost with a skewed follower graph |
| Chat | ordered delivery and connection state |
| Typeahead | top-k freshness under a write-heavy query log |
| Ride matching | proximity search on moving points |
| Payments and booking | exactly-once effects on money and inventory |
| Metrics pipeline | ingest volume and storage over time |

Propose your deep dive, and let the interviewer redirect you. "The interesting part is key generation; I'd like to go deep there unless you'd rather I look at the redirect path." That sentence is itself an SDE-2 signal.

### Labelling the diagram

Label every arrow with its request rate and every store with its size. An unlabelled diagram cannot be reasoned about; a labelled one lets the interviewer see immediately that the 3,858 reads per second go through a cache with a stated hit rate, and that the database behind it sees only the misses.

### What an SDE-2 answer adds

For each component you place, answer three questions without being asked:

1. **Why this and not the obvious alternative?** "A key-value store, because every read is a point lookup by key; a relational database would work, but nothing here needs a join."
2. **What happens when it fails?** "If the cache cluster is lost, the database sees 3,858 reads per second, which one replica set can serve; latency rises, availability holds."
3. **What breaks first at 10x?** "The key-generation service, if it is a single counter; that is why I'd hand out ranges."

## Failure modes and common mistakes

- **Boxes before numbers.** A design whose size is unknown cannot be defended.
- **Technology names as answers.** "Use Kafka" says nothing unless you say what ordering, retention, and consumer model you need from it.
- **One-size consistency.** Treating every read as strongly consistent, or every read as eventually consistent. Say which data needs which.
- **Deep-diving the easy part.** Designing the load balancer in detail while the key-generation question goes unanswered.
- **No failure story.** Every stateful component needs a sentence on what happens when it is lost.
- **Silent assumptions.** Assuming a number without saying so; the interviewer cannot tell a deliberate assumption from a mistake.

## Interview questions and model answers

**Q: Design X. Where do you start?**
A: With requirements on the board: three to five functional ones, the non-functional ones including latency, consistency and durability, and two or three non-goals. Then a five-minute estimate of load, storage and read/write ratio, because those numbers choose the architecture.

**Q: Why estimate at all if the numbers are rough?**
A: Because the order of magnitude decides the design. Forty writes per second and three terabytes means one database can hold everything and the problem is availability and key generation. Four hundred thousand writes per second means sharding is the first decision. Being wrong by a factor of two changes nothing; skipping the estimate can change everything.

**Q: How do you pick what to go deep on?**
A: I ask which requirement is hardest at the estimated load and propose that component, then let the interviewer redirect me. For a feed, that is fan-out with a skewed follower graph. For payments, it is exactly-once effects.

**Q: What would you monitor?**
A: The SLI for each user-facing flow - latency and error rate - plus the saturation signal of the component I expect to break first at 10x: cache hit rate, consumer lag, replication lag, or connection count.

## Exercises

1. Write requirements, non-goals and a five-minute estimate for "design a pastebin" before reading chapter 2.
2. For each prompt in the deep-dive table, name the requirement that makes that component hard.
3. Take any past design you have built at work and label every arrow with its request rate. Which number surprised you?

## Chapter summary

A high-level design is data flows sized against load, with a failure behaviour for each. Run a fixed 45-minute structure, write requirements and non-goals down, and estimate before drawing: the order of magnitude chooses the architecture. Spend the deep dive where the hardest requirement meets the load, and for every component say why, what happens when it fails, and what breaks first at 10x.

## Revision checklist

- [ ] I can recite the 45-minute structure and its time budget.
- [ ] I can estimate QPS, storage and bandwidth from users and ratios in under five minutes.
- [ ] I state non-goals explicitly.
- [ ] I can name the hard component for each prompt in this book.
- [ ] For every component, I say why, how it fails, and what breaks at 10x.
