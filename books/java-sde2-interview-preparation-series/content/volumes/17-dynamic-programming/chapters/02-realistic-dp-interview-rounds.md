# Realistic Dynamic Programming Interview Rounds

## Round 1: minimum coins for an amount

### Prompt

Given positive coin denominations with unlimited reuse, return the fewest coins needed to form `amount`, or -1.

### Candidate derivation

State: `dp[a]` is the minimum coins required to form exact amount `a`; unreachable states use an infinity sentinel. Base `dp[0] = 0`. For each amount, try every coin that can be the final coin.

```java
static int minimumCoins(int[] coins, int amount) {
    if (amount < 0) throw new IllegalArgumentException("negative amount");
    int unreachable = amount + 1;
    int[] dp = new int[amount + 1];
    Arrays.fill(dp, unreachable);
    dp[0] = 0;
    for (int current = 1; current <= amount; current++) {
        for (int coin : coins) {
            if (coin <= 0) throw new IllegalArgumentException("positive coins required");
            if (coin <= current && dp[current - coin] != unreachable) {
                dp[current] = Math.min(dp[current], dp[current - coin] + 1);
            }
        }
    }
    return dp[amount] == unreachable ? -1 : dp[amount];
}
```

### Follow-up answers

**Why `amount + 1` as infinity?** With positive integer coins, any feasible solution uses at most `amount` coins when denomination 1 exists, and the sentinel avoids overflow from `Integer.MAX_VALUE + 1`. Without coin 1, it remains safely above every possible coin count.

**Need the actual coins?** Store the last chosen coin for each improved amount and walk backward. Define tie-breaking among equal-length solutions.

**Complexity?** O(amount * numberOfCoins) time and O(amount) space; pseudo-polynomial in numeric amount.

## Round 2: longest common subsequence

### Prompt

Return the length of the longest sequence appearing in order, not necessarily contiguously, in both strings.

### State and transition

```text
dp[i][j] = LCS length of first i chars of first and first j chars of second
```

If final characters match, extend `dp[i-1][j-1]`. Otherwise discard one final character and keep the better of `dp[i-1][j]` and `dp[i][j-1]`.

```java
static int lcsLength(String first, String second) {
    int[][] dp = new int[first.length() + 1][second.length() + 1];
    for (int i = 1; i <= first.length(); i++) {
        for (int j = 1; j <= second.length(); j++) {
            if (first.charAt(i - 1) == second.charAt(j - 1)) {
                dp[i][j] = dp[i - 1][j - 1] + 1;
            } else {
                dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
    }
    return dp[first.length()][second.length()];
}
```

### Follow-up answers

**Substring instead?** Contiguous matching resets on mismatch; the state and answer aggregation differ.

**Space compression?** Length needs only previous and current rows: O(min(m,n)) space after choosing the shorter string as columns. Reconstruction generally needs the full table or a divide-and-conquer method.

**Unicode?** `charAt` compares UTF-16 code units. If the contract is code-point sequences, convert accordingly; user-perceived grapheme sequences need a stronger text boundary.

## Round 3: equal-subset partition

### Prompt

Return whether positive integers can be partitioned into two groups with equal sum.

### Candidate derivation

The total must be even. The problem becomes 0/1 subset sum for target `total / 2`.

```java
static boolean canPartition(int[] values) {
    long total = 0L;
    for (int value : values) {
        if (value < 0) throw new IllegalArgumentException("nonnegative values required");
        total += value;
    }
    if ((total & 1L) != 0L || total / 2L > Integer.MAX_VALUE) return false;
    int target = (int) (total / 2L);
    boolean[] possible = new boolean[target + 1];
    possible[0] = true;
    for (int value : values) {
        for (int sum = target; sum >= value; sum--) {
            possible[sum] |= possible[sum - value];
        }
    }
    return possible[target];
}
```

### Follow-up answers

**Why downward?** Each value may be used once. Upward iteration could read a state written earlier in the same item iteration and reuse that item.

**Large values?** O(n * target) may be infeasible even for moderate n. A meet-in-the-middle approach can suit small n with huge values; a bitset can improve constants for bounded sums; approximation depends on the business contract.

## Closing answer pattern

Say the state sentence, choices, recurrence, bases, evaluation order, answer location, state-count complexity, numeric sentinel policy, reconstruction plan, and whether compression preserves required information.
