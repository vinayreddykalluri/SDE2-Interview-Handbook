# Boundary Engineering and Search on Answers

Binary search is not “look at the middle until found.” It is maintaining a boundary between two regions whose truth is known. Once the interval convention and predicate are explicit, exact lookup, lower/upper bounds, rotated arrays, matrix search, and capacity optimization become variations of one proof.

The complete Java 21 implementations are in `BinarySearchInterviewChecks.java`.

## Prefer a half-open insertion boundary

Lower bound returns the first index whose value is at least target. Search `[left,right)`, initially `[0,n)`:

```text
[0,left)   values < target
[right,n)  values >= target
[left,right) unknown
```

At `middle`:

- if `values[middle] < target`, discard through middle: `left = middle + 1`;
- otherwise, middle may be the first valid index: `right = middle`.

When `left == right`, no unknown index remains and that boundary is the answer. It may equal `n`, which is a valid insertion position but not a valid array index.

Upper bound changes only equality: it finds the first value strictly greater than target. The half-open equal range is `[lowerBound, upperBound)`, and count is their difference.

## Why loop updates must exclude something

Every branch must shrink the unknown interval. In a half-open first-true search, `right = middle` is safe because middle remains a candidate while width decreases; `left = middle + 1` excludes a proven-false middle.

Mixing a `while (left < right)` loop with `right = middle - 1` often skips candidates. Mixing inclusive and exclusive meanings is the source of most “almost works” binary searches.

Write the invariant above the loop before code.

## Midpoint overflow

For nonnegative array indexes:

```java
int middle = left + (right - left) / 2;
```

avoids `left + right` overflow. A fully signed `long` domain can make even `high - low` overflow. The companion's reusable `firstTrue` uses an overflow-safe signed average:

```java
(low & high) + ((low ^ high) >> 1)
```

Most interview answer domains are nonnegative, where the ordinary difference formula is clearer. Use the stronger formula only when the broader contract needs it.

## Search on answer

Some prompts do not provide a sorted array. Instead, candidate answers have a monotonic feasibility predicate:

```text
capacity too small: false false false
capacity sufficient:              true true true
                                first true is optimum
```

Shipping capacity uses lower bound `max(weight)` and upper bound `sum(weights)`. “Can ship within D days at capacity C” becomes no harder as C grows. The binary search finds the first feasible capacity.

Before searching, prove:

1. the answer domain contains the optimum;
2. feasibility is monotonic;
3. the chosen endpoint is guaranteed feasible; and
4. the predicate does not overflow.

For eating speed, hours per pile use ceiling division:

```text
(pile + speed - 1) / speed
```

with promotion to `long` before addition.

## Integer square root without multiplication overflow

To test whether `middle * middle <= value`, use:

```text
middle == 0 || middle <= value / middle
```

The companion searches for the last valid middle and correctly handles `Long.MAX_VALUE`, whose floor square root is `3_037_000_499`.

This is a boundary problem: true values form a prefix and false values a suffix. Returning the saved last-true answer avoids confusing it with first-true code.

## Rotated distinct array

In a sorted array rotated once, at least one half around middle is sorted when values are distinct.

```text
[4,5,6,7,0,1,2]
 left half sorted when values[left] <= values[middle]
```

If target lies inside the sorted half's inclusive/exclusive value boundaries, keep that half; otherwise keep the other. With duplicates, `left`, `middle`, and `right` can be equal and reveal no sorted side. A duplicate-tolerant variant may shrink endpoints, degrading to `O(n)` worst case. State which contract you implement.

## Flattened matrix search

A rectangular matrix is searchable as one sorted sequence only if:

- each row is sorted; and
- the first value of each row is greater than the last of the previous row.

Map flat index `p` to `row = p / columns`, `column = p % columns`. Compute total cells in `long` before multiplying row and column counts. A matrix with rows sorted independently but overlapping ranges needs a different strategy.

## Edge-case matrix

| Case | Correct handling | Common failure |
|---|---|---|
| empty sorted array | lower/upper bound return zero | inspect index zero |
| target below all | boundary zero | return missing sentinel only |
| target above all | boundary `n` | dereference insertion position |
| many duplicates | lower and upper equality differ | arbitrary matching index |
| full signed long domain | overflow-safe midpoint | `high-low` overflow |
| answer high not feasible | reject/expand bound | return a false endpoint |
| predicate not monotonic | do not binary search | relying on samples |
| sum/capacity overflow | aggregate in `long` | overflow before assignment |
| square-root multiplication | compare by division | negative overflow looks feasible |
| rotated duplicates | use duplicate-aware contract | assuming one strict sorted half |
| empty matrix row | return false under rectangular contract | divide by zero |
| jagged matrix | reject or search rows separately | flat-index wrong row |

## Six live interview Q&A chains

### 1. Lower-bound result `n`

**Interviewer:** Your search returned array length. Is that a bug?

**Candidate:** Not for lower bound. It means every value is smaller and insertion belongs after the array. I check `index < n` before dereferencing for exact-match logic.

### 2. Equality branch

**Interviewer:** Why does lower bound move right on equality?

**Candidate:** Equality satisfies “at least target,” but an earlier equal value may exist. Middle remains a candidate, so `right = middle`.

### 3. Answer predicate

**Interviewer:** How do you know shipping feasibility is monotonic?

**Candidate:** Increasing capacity never forces an additional day; the same packing remains possible or improves. Therefore false capacities precede true capacities.

### 4. High endpoint

**Interviewer:** Why use total weight as capacity high?

**Candidate:** With positive weights, that capacity ships everything in one day, so it is feasible for any positive day limit. A guaranteed true high is required by my first-true helper.

### 5. Rotated duplicates

**Interviewer:** Does your rotated search support duplicates?

**Candidate:** The companion intentionally requires distinct values. With equal left/middle/right, sorted-half identification is ambiguous; a robust duplicate version shrinks boundaries and can become linear.

### 6. Testing boundaries

**Interviewer:** How would you test lower and upper bound?

**Candidate:** Target before all, after all, equal at each edge, all duplicates, empty, and singleton. Then generate sorted duplicate-heavy arrays and compare both boundaries with linear scans. The companion runs that deterministic differential test.

## Run the companion

```bash
javac --release 21 -Xlint:all -Werror BinarySearchInterviewChecks.java
java BinarySearchInterviewChecks
```

Expected final line: `PASS 18 binary-search checks`.
