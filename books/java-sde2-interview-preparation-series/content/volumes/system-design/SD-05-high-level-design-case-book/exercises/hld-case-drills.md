# High-Level Design Case Drills

Work these in order. Each drill names the artifact to produce. Write your answer before reading the solution; the gap between the two is the lesson. Show arithmetic, and round in the direction that makes the design safer.

## Estimation and allocation

**D1. Scale the shortener tenfold.**
The URL shortener from chapter 2 now receives 1 billion new links per month, still 100 reads per write, 500 bytes per record, 5 years of retention.
*Produce:* average writes and reads per second, records and storage over five years, and the one design decision that changes because of the new numbers.

**D2. Size a random key.**
At 100 million new links per month for five years, random base62 keys must have fewer than one expected collision in total.
*Produce:* the minimum key length, with the birthday arithmetic for the lengths either side of it.

## Partitioning and skew

**D3. Resize a cluster.**
A cache cluster grows from 10 to 12 nodes.
*Produce:* the fraction of keys that change owner under `hash mod N`, the ideal fraction under consistent hashing, and one sentence on what the difference means for the database behind the cache.

**D4. Choose quorums.**
A replicated store has `N = 3`.
*Produce:* `R` and `W` for a shopping cart, a session token, and an account's email address, each with the consistency behaviour it gives.

**D5. Move the feed threshold.**
In the Zipf follower model from chapter 4 (1,000,000 accounts, the rank-`r` account has `top / r` followers), the top account grows from 10 million to 100 million followers.
*Produce:* the number of accounts above 10,000 and above 100,000 followers, and the share of fan-out writes each threshold removes, for both values of `top`. Say whether you would move the threshold.

## Ordering and delivery

**D6. Size the gateways.**
50 million concurrent chat users, 200,000 connections per gateway, three availability zones.
*Produce:* the number of gateways needed to serve the load, and the number needed to survive the loss of one zone.

**D7. Trace the receiver.**
A conversation receiver has delivered up to sequence 7. Arrivals: 9, 8, 8, 11, 10, 12.
*Produce:* what is delivered after each arrival, which gaps are requested, and which arrivals are dropped as duplicates.

## Precomputation, spatial data, and volume

**D8. Size a crawler's Bloom filter.**
A crawler must remember 1 billion URLs with a 0.1% false-positive rate.
*Produce:* bits, memory, and number of hash functions, plus what a false positive costs the crawler.

**D9. Pick a geohash precision.**
"Restaurants within 200 metres" in a city near the equator.
*Produce:* the precision, the cell size, the number of cells a query reads, and why.

**D10. Size a larger metrics system.**
20 million series at 15-second resolution, with the tiering from chapter 9 (15 s for 14 days, 1 minute for 90 days, 1 hour for a year) and 1.37 bytes per compressed point.
*Produce:* points per day, raw and compressed GB per day, compressed storage for a year at full resolution, and tiered storage.

**D11. Compare durability schemes.**
Per-node failure probability within one repair window: 1%.
*Produce:* the object loss probability and overhead for 2x replication, 3x replication, RS(10,4), and RS(12,4), and the scheme you would choose for a backup tier.

## Challenge

**D12. Full mock: design a ticketing platform for stadium concerts.**
50,000 seats per show, 2 million users arriving when a popular sale opens, seat maps, holds for 10 minutes, and payment.
*Produce:* requirements and non-goals, the estimate, the high-level design, one deep dive of your choice, the follow-up answers for 10x load and region loss, and your rubric score out of twelve.
