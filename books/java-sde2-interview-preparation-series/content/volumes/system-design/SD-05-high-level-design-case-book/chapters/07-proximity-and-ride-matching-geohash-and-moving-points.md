# 7. Proximity and Ride Matching: Geohash and Moving Points

## Learning objectives

By the end of this chapter, you should be able to:

- explain why a two-column latitude/longitude index cannot answer "nearby" efficiently;
- encode locations as geohashes and choose a precision from the cell sizes;
- handle the boundary problem that makes a single-cell lookup wrong;
- design a location service for frequently moving drivers, sized by update rate; and
- design the matching step so that one driver is never assigned to two riders.

## Why this matters at SDE-2

"Design Uber", "design Yelp", and "find nearby friends" all reduce to the same question: given a point, find the things near it, fast, while those things may be moving. The interviewer is checking whether you know a spatial indexing technique, whether you know its boundary flaw, and whether you notice that a moving fleet makes this a write-heavy problem rather than a read-heavy one.

## First-principles model

Proximity search needs **a one-dimensional key that keeps nearby points close together**, so that an ordinary sorted index or hash map can find neighbours.

- **Static places** (restaurants): written rarely, read often. Any spatial index works; cache aggressively.
- **Moving drivers:** written every few seconds per driver, read on every ride request. The index must absorb a high update rate, and exact durability of every position does not matter - the next update replaces it.
- **Matching** is a separate, transactional step: choose among the nearby candidates and reserve exactly one.

## Core terminology

- **Geohash:** a base32 string made by interleaving the bits of longitude and latitude; shared prefixes mean nearby cells.
- **Precision:** geohash length; each extra character shrinks the cell by a factor of 32.
- **Boundary problem:** two points metres apart can have geohashes with a short common prefix.
- **Quadtree:** a tree that splits space into four quadrants, subdividing dense areas more finely.
- **H3 / S2:** hierarchical hexagonal (H3) and spherical (S2) cell systems used in production.
- **Location update:** a driver's periodic position report.

## Detailed mechanics

### Why two B-tree columns are not enough

An index on latitude finds everything in a horizontal band - a strip across the whole planet. An index on longitude finds a vertical band. The database can intersect them, but each band may contain millions of rows, so "nearby" becomes an expensive scan. The fix is a key that encodes both dimensions together.

### Geohash

Geohash alternately halves the longitude and latitude ranges, recording one bit per halving, and emits one base32 character per five bits. Every extra character narrows the cell. The companion computes cell sizes at the equator:

```text
precision   cell at the equator (width x height)
4           39.1 km x 19.6 km
5           4.9 km x 4.9 km
6           1.2 km x 611 m
7           153 m x 153 m
8           38 m x 19 m
```

and encodes a real point:

```text
San Francisco (37.7749, -122.4194) -> 9q8yyk8y
```

The rule for choosing: **pick the finest precision whose cells are at least as large as the search radius in both directions.** Only then does the rider's cell plus its eight neighbours (next section) cover every point within the radius. For "drivers within 500 metres", precision 6 works: its cells are at least 611 m on each side. For "within a kilometre", precision 6 is too fine - a cell is only 611 m tall - so use precision 5, or keep precision 6 and search a 5 x 5 block. Cells also shrink in width away from the equator, because lines of longitude converge, so choose precision by the latitude you serve.

### The boundary problem

Geohash puts nearby points in the same cell *most of the time*. At a cell edge it does not:

```text
two points 5.3 m apart across a cell edge: 9q8yyk vs 9q8yys (5 shared chars)
```

Two points five metres apart fall into different precision-6 cells. A search that looks only in the rider's own cell misses the driver across the street. The standard fix is to search the rider's cell **and its eight neighbours**, then filter the candidates by true distance. Neighbour cells are computed directly from the geohash, so this is nine lookups, not a scan - and it is only complete if the precision follows the rule above, so that no point within the radius lies beyond the neighbouring cells.

### Alternatives worth naming

- **Quadtree:** subdivides dense areas more finely, so each leaf holds a bounded number of points. Good for static data with very uneven density; harder to update at high write rates.
- **S2 and H3:** production-grade cell systems. S2 maps the sphere onto a cube and uses a space-filling curve; H3 uses hexagons, whose neighbours are all the same distance away, which simplifies "ring around this cell" searches. Mention that real ride-hailing systems use systems like these; the geohash reasoning carries over.

### The location service

In scope: drivers report location; riders request a ride; the system finds nearby available drivers and assigns one. Out of scope: pricing, routing, and payments (chapter 8).

The write rate is the design driver. With, say, 1 million active drivers reporting every 4 seconds, the service absorbs 250,000 location updates per second. Each update:

1. goes to an in-memory store keyed by driver id, holding the latest position and status;
2. moves the driver between cell sets if the geohash cell changed - most updates do not change cell at precision 6, which keeps index churn low;
3. is appended to a stream for analytics and trip records, off the hot path.

Positions are soft state: if a node is lost, drivers re-report within seconds. Do not put every location update into a durable transactional database.

Partition the in-memory index by geography - by coarse cell or by city - so that a ride request touches one partition and its neighbours. Dense cities need finer partitions than rural areas; that unevenness is the case for a quadtree-like split or for sizing partitions by load rather than by area.

### Matching without double assignment

A ride request finds candidates in the rider's cell and neighbours, filters by true distance, and ranks by estimated time to pickup. Then it must reserve one driver. Two riders may pick the same driver at the same moment, so reservation is a conditional update - the same check-then-act pattern as seat booking in chapter 8:

```text
set driver.status = RESERVED, driver.trip = T  only if driver.status == AVAILABLE
```

If the update fails, take the next candidate. Offer the trip to the driver with a timeout; if the driver declines or the timeout fires, release the reservation and try the next one.

## Failure modes and common mistakes

- **Latitude and longitude as two independent indexes.** Each one returns a planet-wide band.
- **Searching only the rider's cell.** Misses drivers just across an edge.
- **Durable storage for every location update.** Position is soft state; the write rate makes this expensive for no gain.
- **Choosing precision without checking it against the radius.** If a cell is smaller than the search radius, the 3 x 3 search misses points; precision 5 is about 5 km, precision 7 about 150 m.
- **Assignment without a conditional update.** Two riders get the same driver.

## Interview questions and model answers

**Q: How do you find drivers near a rider?**
A: Geohash the positions and index drivers by cell, at the finest precision whose cells are at least the search radius on each side. For a 500-metre radius that is precision 6, whose cells are about 1.2 km by 0.6 km at the equator. Query the rider's cell and its eight neighbours, then filter by true distance.

**Q: Why the neighbours?**
A: Because nearby points can fall in different cells. In the companion, two points 5 metres apart straddle a precision-6 edge and share only five characters. Without the neighbours you miss the closest driver.

**Q: What is the hardest load in this system?**
A: Location updates. A million drivers every four seconds is 250,000 writes per second. I keep positions in partitioned in-memory stores as soft state, update the cell index only when a driver changes cell, and stream updates for analytics off the hot path.

**Q: How do you stop two riders getting the same driver?**
A: Reservation is a conditional update from available to reserved. The second request's update fails, so it takes the next candidate.

## Exercises

1. Pick a precision for "restaurants within 200 metres" and say how many cells a query touches.
2. Estimate memory for the driver location store: 1 million drivers, with id, position, status, and cell.
3. Design surge detection: counting requests and available drivers per cell per minute.

## Chapter summary

Proximity search needs a key that encodes both dimensions; geohash provides one, with precision chosen from cell size. Nearby points can straddle a cell edge, so search the cell and its eight neighbours and filter by distance. A moving fleet makes location a write-heavy, soft-state problem sized by update rate. Matching reserves a driver with a conditional update so that no driver is assigned twice.

## Revision checklist

- [ ] I can explain why two independent indexes cannot answer "nearby".
- [ ] I can pick a geohash precision so that the 3 x 3 search covers the radius.
- [ ] I can demonstrate the boundary problem and its fix.
- [ ] I can size the location-update rate and explain why positions are soft state.
- [ ] I can reserve a driver without double assignment.
