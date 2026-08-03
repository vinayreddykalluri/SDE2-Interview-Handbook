# Chapter 12: Powers, Roots, and Logarithms for Complexity Reasoning

Powers, roots, and logarithms appear in interviews less as abstract mathematics and more as a language for counting work. A candidate who can translate repeated doubling, repeated halving, and tree height into precise complexity can reason about binary search, heaps, divide-and-conquer algorithms, capacity growth, and integer boundary problems without memorizing isolated formulas.

This chapter focuses on that translation. It also develops overflow-safe integer square-root and perfect-square checks, where a mathematically correct idea can still fail in Java if an intermediate multiplication overflows or a floating-point approximation is trusted without verification.

## 12.1 Learning objectives

After this chapter, you should be able to:

- recognize powers of two in capacities, binary representations, and balanced trees;
- explain why repeated doubling or halving produces a logarithmic number of rounds;
- distinguish floor(log2(n)) from ceil(log2(n));
- derive binary-search and balanced-tree height bounds;
- compare linear, polynomial, and exponential growth;
- compute an integer square root without multiplication overflow;
- verify whether an integer is a perfect square;
- explain why Math.sqrt is useful as an estimate but not always sufficient as proof;
- choose long intermediates and division-based guards deliberately.

## 12.2 Powers of two are the native scale of binary algorithms

![Powers of two connect repeated doubling, repeated halving, fixed-width bounds, and logarithms.](content/volumes/dsa/DSA-02-03-number-systems-and-math-foundations/assets/09-powers-of-two-scale.png)

A power of two has the form 2^k for a nonnegative integer k. Each increment of k doubles the value.

| k | 2^k | Interview connection |
|---:|---:|---|
| 0 | 1 | One state or one leaf |
| 1 | 2 | One binary choice |
| 5 | 32 | Bits in an int |
| 10 | 1,024 | Approximate one thousand |
| 20 | 1,048,576 | Approximate one million |
| 30 | 1,073,741,824 | Largest positive power of two that fits in a positive int |
| 31 | 2,147,483,648 | One more than Integer.MAX_VALUE; not a positive int |
| 63 | 9,223,372,036,854,775,808 | One more than Long.MAX_VALUE; not a positive long |

The last two rows matter because a mathematical power can exist while its positive representation does not fit the selected Java type. The expression 1 << 31 is an int operation and produces Integer.MIN_VALUE, not the positive mathematical value 2^31.

### Recognition signals

Expect powers of two when a problem mentions:

- binary choices or a complete binary tree;
- doubling capacity;
- repeatedly splitting a range in half;
- bit positions or masks;
- array-backed heaps;
- a search space of all subsets;
- a requirement to find the next power-of-two capacity.

## 12.3 Repeated doubling and ceil(log2(n))

Suppose a buffer starts with capacity 1 and doubles until it can hold n elements:

~~~text
1 -> 2 -> 4 -> 8 -> 16 -> ... -> at least n
~~~

After r doublings, the capacity is 2^r. The first sufficient r satisfies:

~~~text
2^r >= n
~~~

Therefore:

~~~text
r = ceil(log2(n))
~~~

For n = 13, the capacities are 1, 2, 4, 8, 16. Four doublings are required, and ceil(log2(13)) is 4.

The distinction between floor and ceiling is practical:

- floor(log2(n)) is the index of the highest set bit for positive n;
- ceil(log2(n)) is the number of doublings needed to reach at least n;
- when n is already a power of two, the two values are equal;
- otherwise, the ceiling is one more than the floor.

Do not compute these values with floating-point logarithms when an exact integer answer controls allocation or indexing. Integer loops or bit operations avoid rounding ambiguity.

## 12.4 Repeated halving and floor(log2(n))

![Fast exponentiation uses the binary form of the exponent to choose squared factors in logarithmic time.](content/volumes/dsa/DSA-02-03-number-systems-and-math-foundations/assets/16-fast-exponentiation.png)

Now start with a positive integer n and repeatedly divide it by two using integer division until it becomes 1:

~~~text
37 -> 18 -> 9 -> 4 -> 2 -> 1
~~~

There are five halvings. Since 2^5 <= 37 < 2^6, floor(log2(37)) is 5.

This pattern appears in:

- binary search;
- heap movement from a node to its parent;
- balanced-tree height;
- exponentiation by squaring;
- divide-and-conquer recurrences;
- algorithms that discard at least half of the remaining candidates.

The base of a logarithm usually does not change Big-O notation. log2(n), log10(n), and log8(n) differ by constant factors. In an interview, however, base 2 often explains the actual mechanism, so saying "logarithmic because each step halves the remaining range" is stronger than merely stating O(log n).

The same halving model powers exponentiation by squaring. If the remaining exponent is odd, multiply the result by the current factor. Square the factor, halve the exponent, and repeat. This reduces O(exponent) multiplications to O(log exponent); exact or modular multiplication still needs an explicit overflow policy.

Factorial follow-ups use different counting ideas. Trailing zeros in `n!` equal the total number of factors of five: `n/5 + n/25 + n/125 + ...`. The number of decimal digits is `floor(log10(n!)) + 1`, and `log10(n!)` is the sum of `log10(k)` for `k` from 2 through `n`. These are SDE-2 follow-ups, not prerequisites for the main root and logarithm path.

## 12.5 Why binary search is O(log n)

Let the current search range contain n candidates. One comparison keeps at most about n/2 candidates. After k comparisons, the remaining size is at most:

~~~text
n / 2^k
~~~

The search stops when at most one candidate remains:

~~~text
n / 2^k <= 1
2^k >= n
k >= log2(n)
~~~

Thus binary search uses O(log n) comparisons.

For an array of one billion elements, a linear scan may inspect one billion positions. Binary search needs at most about 30 range reductions because 2^30 is slightly greater than one billion. This is the interview intuition worth retaining.

> **Interview explanation:** Binary search is logarithmic because every comparison removes at least half of the remaining ordered search space. After k steps, no more than n / 2^k candidates remain.

The proof depends on a valid monotonic or sorted predicate. Halving an invalid search space does not make an incorrect algorithm correct.

## 12.6 Tree height and powers of two

A perfect binary tree with root at level 0 has 2^d nodes at depth d. A tree with height h, measured in edges, contains:

~~~text
1 + 2 + 4 + ... + 2^h = 2^(h + 1) - 1
~~~

Solving approximately for h gives h = O(log n).

This height bound supports logarithmic search, insertion, or deletion only when the tree remains balanced. A plain binary search tree can degenerate into a chain:

~~~text
1
 \
  2
   \
    3
     \
      4
~~~

That tree has height n - 1, so search becomes O(n). A heap remains complete and therefore has O(log n) height, even though it does not provide binary-search-tree ordering.

### Common height conventions

Interviewers use two conventions:

- height in edges: a leaf has height 0;
- height in nodes: a leaf has height 1.

State your convention before deriving a formula. The asymptotic result is unchanged, but exact answers differ by one.

## 12.7 Exponential growth outruns polynomial growth

An exponential function such as 2^n multiplies by a fixed factor when n increases by one. A polynomial such as n^3 grows much more slowly for large n.

| n | n^2 | 2^n |
|---:|---:|---:|
| 10 | 100 | 1,024 |
| 20 | 400 | 1,048,576 |
| 30 | 900 | 1,073,741,824 |
| 40 | 1,600 | 1,099,511,627,776 |

This explains why generating all subsets is expensive. A set of n elements has 2^n subsets because each element creates two choices: include or exclude.

Exponential algorithms may still be appropriate when:

- n is deliberately small;
- pruning removes most branches;
- the problem asks for every result, so output size is itself exponential;
- an exact solution is required and no polynomial algorithm is known;
- memoization can collapse repeated states into a smaller state space.

Do not call an algorithm O(2^n) only because it uses recursion. A single recursive call on n - 1 is O(n) depth and may be O(n) total work. The branching structure and repeated work determine the complexity.

## 12.8 Integer square root

For a nonnegative integer n, the floor square root is the largest integer r such that:

~~~text
r * r <= n
~~~

A brute-force algorithm tests 0, 1, 2, and so on, taking O(sqrt(n)) time. Binary search reduces this to O(log n).

The obvious comparison mid * mid <= n is dangerous when mid is an int or long. The product can overflow before it is compared. For positive mid, use:

~~~text
mid <= n / mid
~~~

Division stays within the type's range and expresses the same inequality.

### Overflow-safe Java implementation

~~~java
public final class IntegerRoots {
    private IntegerRoots() {
    }

    public static long floorSquareRoot(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be nonnegative");
        }
        if (value < 2) {
            return value;
        }

        long low = 1;
        long high = Math.min(value / 2 + 1, 3_037_000_499L);
        long answer = 1;

        while (low <= high) {
            long mid = low + (high - low) / 2;
            if (mid <= value / mid) {
                answer = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return answer;
    }

    public static boolean isPerfectSquare(long value) {
        if (value < 0) {
            return false;
        }
        long root = floorSquareRoot(value);
        return root != 0 ? value / root == root && value % root == 0
                         : value == 0;
    }

    public static int floorLog2(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("value must be positive");
        }
        int result = -1;
        while (value > 0) {
            value /= 2;
            result++;
        }
        return result;
    }

    public static int ceilLog2(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("value must be positive");
        }
        if (value == 1) {
            return 0;
        }
        return floorLog2(value - 1) + 1;
    }

    public static void main(String[] args) {
        System.out.println(floorSquareRoot(27));  // 5
        System.out.println(isPerfectSquare(81)); // true
        System.out.println(floorLog2(37));        // 5
        System.out.println(ceilLog2(37));         // 6
    }
}
~~~

The root bound 3,037,000,499 is floor(sqrt(Long.MAX_VALUE)). The algorithm would also remain correct with high = value / 2 + 1, but the tighter bound avoids searching candidates that cannot be valid long square roots.

**Time complexity:** O(log n) iterations for the square-root search and O(log n) divisions for the loop-based logarithms.

**Space complexity:** O(1).

**Numeric safety:** Midpoint calculation uses low + (high - low) / 2. The square comparison uses division, not multiplication. The value - 1 expression in ceilLog2 is safe because value is positive.

### Dry run for floorSquareRoot(27)

| low | high | mid | mid <= 27 / mid | action |
|---:|---:|---:|:---:|---|
| 1 | 14 | 7 | false | high = 6 |
| 1 | 6 | 3 | true | answer = 3, low = 4 |
| 4 | 6 | 5 | true | answer = 5, low = 6 |
| 6 | 6 | 6 | false | high = 5 |

The loop ends with answer 5. Since 5^2 <= 27 and 6^2 > 27, 5 is the floor square root.

## 12.9 Why Math.sqrt may need verification

Math.sqrt returns a double. It is excellent for numerical work and can provide a fast candidate root, but a coding-interview method that needs an exact integer property should verify the candidate.

There are two concerns:

1. A long may be larger than 2^53, beyond the range where every integer is exactly representable as a double.
2. Converting the approximate square root back to long may produce a candidate one step below or above the exact floor near a rounding boundary.

A safe hybrid strategy is:

~~~java
public final class VerifiedSquareRoot {
    private VerifiedSquareRoot() {
    }

    public static long floorSquareRoot(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be nonnegative");
        }
        long candidate = (long) Math.sqrt(value);

        while (candidate > 0 && candidate > value / candidate) {
            candidate--;
        }
        while (candidate < 3_037_000_499L
                && candidate + 1 <= value / (candidate + 1)) {
            candidate++;
        }
        return candidate;
    }

    public static boolean isPerfectSquare(long value) {
        if (value < 0) {
            return false;
        }
        long root = floorSquareRoot(value);
        return root == 0
                ? value == 0
                : value / root == root && value % root == 0;
    }
}
~~~

**Time complexity:** O(1) for practical fixed-width long values after Math.sqrt, with a small verification adjustment. If an interviewer wants a proof independent of floating-point behavior, prefer the binary-search version and state O(log n).

**Space complexity:** O(1).

The verification still avoids root * root. A candidate close to sqrt(Long.MAX_VALUE) can be squared safely only with additional guards.

## 12.10 Complexity connections worth explaining aloud

| Mechanism | Number of rounds | Typical example |
|---|---:|---|
| Add or remove one item per round | O(n) | Linear scan |
| Multiply or divide remaining work by two | O(log n) | Binary search |
| Process all items at every halving level | O(n log n) | Merge sort |
| Try every pair | O(n^2) | Nested full-range loops |
| Make two recursive choices per element | O(2^n) | Subset generation |
| Try permutations | O(n!) | Exhaustive ordering |
| Test divisors through sqrt(n) | O(sqrt(n)) | Basic prime check |
| Binary search an answer in [0, n] | O(log n) | Integer square root |

An O(n log n) explanation should account for both factors. In merge sort, there are O(log n) levels, and each level processes O(n) total elements. In a heap sort, there are O(n) removals and each removal costs O(log n).

## 12.11 Edge cases and common mistakes

- Treating log2(1) as 1. It is 0 because 2^0 = 1.
- Passing zero to a logarithm routine without defining behavior.
- Confusing the highest set-bit index with the number of bits. For positive n, bit length is floor(log2(n)) + 1.
- Using 1 << k when a long result is required. Use 1L << k, and still validate k and overflow.
- Assuming every binary tree has logarithmic height.
- Writing mid * mid <= n and allowing multiplication overflow.
- Using Math.pow for exact integer powers and casting the rounded double.
- Trusting Math.sqrt as an exact proof for arbitrary long inputs.
- Forgetting that exponential output may make exponential time unavoidable.
- Claiming a recursive algorithm is exponential without counting branches.

## 12.12 Interview questions

1. Why does repeated halving lead to logarithmic complexity?
2. What is the difference between floor(log2(n)) and ceil(log2(n))?
3. Why is binary search O(log n), and what prerequisite makes it valid?
4. Why is the height of a balanced binary tree logarithmic?
5. Can a recursive algorithm have O(n) time? Give an example.
6. Why can mid * mid overflow even when n is a valid long?
7. When is Math.sqrt plus verification reasonable?
8. Why does generating every subset require at least Omega(2^n) output work?

## 12.13 Practice set

Do not look at the answer notes until you have written and tested your own solution.

### Quick check

1. How many doublings are needed to grow capacity 1 to at least 70?
2. What are floor(log2(70)) and ceil(log2(70))?
3. A loop divides n by 3 each round. What is its asymptotic complexity?
4. Does a binary tree with n nodes always have O(log n) height?
5. Why is value / mid safer than mid * mid in a square-root search?

### Coding practice

1. **Foundation:** Implement bitLength(long value) for positive values without using string conversion.
2. **Foundation:** Implement isPowerOfTwoByDivision(long value).
3. **Interview Core:** Implement ceilPowerOfTwo(long value), returning an empty result if the answer exceeds Long.MAX_VALUE.
4. **Interview Core:** Reimplement floorSquareRoot for int and test Integer.MAX_VALUE.
5. **SDE-2 Follow-up:** Find the smallest integer speed that completes a workload before a deadline using binary search on the answer.

### Debugging task

The following code can return a false result:

~~~java
static boolean isPerfectSquare(int value) {
    int root = (int) Math.sqrt(value);
    return root * root == value;
}
~~~

Identify its input-domain problem, multiplication risk, and assumption about floating-point conversion. Produce a version with an explicit contract.

### Interview extension

You are given a sorted array whose length is unknown, but an API returns an out-of-range marker. Explain how exponential range expansion followed by binary search finds a target and derive the total complexity.

## 12.14 Delayed answer notes

### Quick-check answers

1. Seven doublings are needed: 1, 2, 4, 8, 16, 32, 64, 128.
2. floor(log2(70)) is 6, and ceil(log2(70)) is 7.
3. O(log n). The base changes only a constant factor.
4. No. An unbalanced tree can have height n - 1.
5. Multiplication can overflow before comparison; division stays within range for positive operands.

### Coding guidance

- bitLength is floorLog2(value) + 1 for positive values.
- A division-based power-of-two check repeatedly divides by 2 and rejects an odd value before reaching 1.
- For ceilPowerOfTwo, double a long only after checking current > Long.MAX_VALUE / 2.
- The int square-root search should promote operands or compare mid <= value / mid.
- Binary search on an answer requires a monotonic feasibility predicate. State the cost of one predicate evaluation separately from the O(log range) search count.

### Debugging resolution

Negative values must be rejected or defined as false. For nonnegative int inputs, root should be verified without relying only on root * root. One valid check is root != 0 ? value / root == root && value % root == 0 : value == 0, after adjusting the Math.sqrt candidate if necessary. A pure integer binary search gives the clearest correctness argument.

### Interview-extension answer

Probe indices 1, 2, 4, 8, and so on until the target cannot be beyond the discovered upper bound. If the target is at index p, expansion takes O(log p) probes. Binary search over that range also takes O(log p), so total time is O(log p) and extra space is O(1).

## 12.15 Chapter summary

- Powers of two model binary choices, capacities, bit positions, and balanced-tree levels.
- Repeated doubling uses ceil(log2(n)) rounds to reach at least n.
- Repeated halving uses floor(log2(n)) rounds to reach 1.
- A logarithmic claim should name the constant-factor reduction in remaining work.
- Exponential growth appears when independent choices multiply the state space.
- Integer square root can be solved in O(log n) time with binary search.
- Division-based comparisons prevent square overflow.
- Floating-point square roots should be verified when an exact integer property matters.

## 12.16 Revision checklist

- [ ] I can derive binary-search complexity instead of memorizing it.
- [ ] I can distinguish floor and ceiling logarithms.
- [ ] I can explain balanced-tree height and the unbalanced counterexample.
- [ ] I can identify exponential branching.
- [ ] I can implement an overflow-safe integer square root.
- [ ] I can verify a perfect square without unsafe multiplication.
- [ ] I can explain when Math.sqrt needs an integer verification step.
