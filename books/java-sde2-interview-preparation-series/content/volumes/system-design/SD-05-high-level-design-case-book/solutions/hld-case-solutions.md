# High-Level Design Case Drills: Worked Solutions

Every number below was computed by a program. Where a solution rests on an assumption the drill does not state, it says so; in an interview, a named assumption is worth more than a confident guess.

## Estimation and allocation

### D1. Scale the shortener tenfold

```text
writes/s   1,000,000,000 / 2,592,000 s   =    385.8 average
reads/s    x 100                          = 38,580   average
records    1e9 x 12 x 5                   = 60,000,000,000
storage    x 500 bytes                    = 30 TB
```

What changes: **storage now needs partitioning.** Thirty terabytes plus replicas is beyond a comfortable single node, so the links table is partitioned by short code - with consistent hashing or a slot table, as in chapter 3. Read traffic of about 39,000 per second on average, and several times that at peak, also makes the cache mandatory rather than an optimisation.

What does not change: counter-range key generation. At 386 writes per second, blocks of 10,000 ids still mean a coordination request every 26 seconds across the whole fleet.

### D2. Size a random key

Expected collisions for `n = 6,000,000,000` keys in a space of `62^L`:

```text
L = 10:  n^2 / (2 x 62^10) = 21.4 expected collisions
L = 11:  n^2 / (2 x 62^11) = 0.35 expected collisions
L = 12:  n^2 / (2 x 62^12) = 0.0056
```

**Eleven characters** is the minimum. Compare with the seven characters a counter needs for the same volume: random keys cost four extra characters to avoid a collision check that a counter never needs. A collision check with retry at seven characters is also valid - but say that it happens about five million times.

## Partitioning and skew

### D3. Resize a cluster

```text
hash mod N, 10 -> 12:   83.3% of keys move
consistent hashing:     2/12 = 16.7% move (the share taken over by the two new nodes)
```

For `mod N`, a key stays only if `h mod 10 == h mod 12`, which holds for 10 of every 60 residues. A cache that loses five-sixths of its placements at once sends almost the full read load to the database at the moment capacity was being added. With a ring, one-sixth of keys miss once and warm up again.

### D4. Choose quorums

- **Shopping cart: `W = 1, R = 1`** (or `W = 1` with merge on read). Availability matters more than strict consistency; concurrent updates are merged, not overwritten, which needs version vectors.
- **Session token: `W = 2, R = 1`** or `W = 1, R = 1` with a short TTL. A token that is briefly missing on one replica causes a re-login, not data loss; choose by how annoying that is.
- **Account email: `W = 2, R = 2`.** `R + W > N`, so a read after a change sees the change. Showing an old email after the user updated it is a visible bug.

### D5. Move the feed threshold

```text
top = 10,000,000 followers (143,435,683 writes/day if all pushed)
  > 10,000 followers:     999 accounts, remove 52% of fan-out writes
  > 100,000 followers:     99 accounts, remove 36%

top = 100,000,000 followers (1,438,773,858 writes/day if all pushed)
  > 10,000 followers:   9,999 accounts, remove 68%
  > 100,000 followers:    999 accounts, remove 52%
```

With the heavier tail, a 100,000-follower threshold now removes the same 52% that 10,000 did before, while pulling for ten times fewer accounts than a 10,000 threshold would. **Move the threshold up** if the read-side cost of merging many pulled accounts matters; keep it at 10,000 if write load is the constraint. The point of the drill is that the threshold is a number chosen from the distribution, not a constant.

## Ordering and delivery

### D6. Size the gateways

```text
load:                       50,000,000 / 200,000 = 250 gateways
survive loss of 1 of 3 AZ:  250 x 3 / 2          = 375 gateways (125 per zone)
```

With 125 per zone, losing one zone leaves 250, exactly the load. Add a margin on top for reconnect storms: when a zone fails, its users all reconnect at once.

### D7. Trace the receiver

Starting state: delivered through 7, so the next expected sequence is 8.

```text
arrive 9   -> buffer 9; gap requested for 8; nothing delivered
arrive 8   -> deliver 8, 9
arrive 8   -> duplicate (8 < next expected 10); dropped
arrive 11  -> buffer 11; gap requested for 10
arrive 10  -> deliver 10, 11
arrive 12  -> deliver 12
delivered in order: 8, 9, 10, 11, 12; gaps requested: 8 and 10; one duplicate dropped
```

## Precomputation, spatial data, and volume

### D8. Size a crawler's Bloom filter

```text
m = -n ln(p) / (ln 2)^2 = 14,377,587,566 bits = 1.80 GB (1,714 MiB)
k = (m / n) ln 2        = 9.97 -> 10 hash functions
```

About 14.4 bits per URL, however long the URLs are. A false positive means the crawler believes it has seen a URL it has not, so it **skips a page it should have crawled** - one in a thousand new URLs is missed. That is acceptable for a general crawler and not for one that must be complete, which would confirm positives against a durable store.

### D9. Pick a geohash precision

**Precision 6**, whose cells are about 1.2 km by 611 m at the equator. The query reads the rider's cell and its **eight neighbours** and filters by true distance.

The tempting answer is precision 7, because its 153-metre cells are closer to the 200-metre radius. It is wrong with a 3 x 3 search: a rider standing at the edge of the centre cell can have a restaurant 200 m away that lies beyond the 153-metre neighbouring cell. The rule is to choose the finest precision whose cells are at least the radius on each side. If precision 6 returns too many candidates to filter, precision 7 with a 5 x 5 block (25 cells, 765 m across) also covers the radius.

### D10. Size a larger metrics system

```text
points/day        20,000,000 x 86,400 / 15  = 115,200,000,000
raw               x 16 bytes                =  1,843 GB/day
compressed        x 1.37 bytes              =    158 GB/day
one year at 15 s, compressed                =   57.6 TB
tiered (15 s 14 d, 1 m 90 d, 1 h 1 y)       =    6.0 TB
```

Most of the tiered total comes from the 90 days of one-minute rollups. If six terabytes is too much, shortening that tier is the next lever.

### D11. Compare durability schemes

```text
2x replication    overhead 2.0x   P(loss) 1.0e-04
3x replication    overhead 3.0x   P(loss) 1.0e-06
RS(10,4)          overhead 1.4x   P(loss) 1.9e-07
RS(12,4)          overhead 1.33x  P(loss) 4.0e-07
```

For a backup tier - large, cold, written once, rarely read - **RS(10,4)**: the best durability here at 1.4x overhead. RS(12,4) saves a little storage for about twice the loss probability. 2x replication is a hundred times worse than 3x, which is why it is rarely used for data that must not be lost. These figures assume independent failures, so fragments must be spread across failure domains to come anywhere near them.

## Challenge

### D12. Full mock: stadium ticketing

**Requirements.** Browse a show's seat map; hold up to 6 seats for 10 minutes; pay; receive tickets. Non-functional: no seat sold twice; the sale must stay up under the opening surge; the seat map may be a few seconds stale. Non-goals: resale, dynamic pricing, and venue management.

**Estimate.** 2 million users arriving over roughly the first minute is about 33,000 arrivals per second - but only 50,000 seats exist, so at most about 8,300 successful holds if everyone takes six. The inventory is tiny; the arrival rate is huge. **The problem is admission control, not storage.**

**High-level design.**

```text
users -> CDN (static seat map, cached for seconds) -> waiting room -> booking service -> inventory DB
                                                                           |
                                                                    payment service (chapter 8)
```

- **Waiting room:** a queue that admits users at a rate the booking service can serve - say a few thousand per second - in arrival order, with a position shown to the user. Admission is a signed, short-lived token.
- **Booking service:** holds seats with a conditional update from `AVAILABLE` to `HELD`, all-or-nothing for multi-seat holds within one transaction, with a 10-minute expiry.
- **Payment:** idempotent, keyed by the hold id; on success the hold becomes a booking.
- **Seat map:** served from a cache refreshed every few seconds. A user can see a seat as available that was just held; the hold attempt fails and the map refreshes.

**Deep dive: contention on the inventory database.** 50,000 seat rows fit on one database. Contention is on individual rows, and conditional updates resolve it correctly - losers retry with other seats. The waiting room keeps the attempt rate bounded, so the database never sees the full surge.

**Follow-ups.** At 10x - 20 million users - only the waiting room grows, because admission caps the rate reaching the booking service; the waiting room itself is a stateless tier over a partitioned queue. Region loss: inventory is replicated synchronously to a second region, because an unreplicated hold or sale is not acceptable; the waiting room restarts from its persisted queue positions, and admitted tokens remain valid.

**Rubric.** Score honestly out of twelve against chapter 10. Common deductions: no admission control (the database takes the whole surge), per-seat holds that are not all-or-nothing, and payment not keyed by the hold.
