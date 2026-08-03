# Realistic Recursion and Backtracking Interview Rounds

## Round 1: subsets with duplicate input values

### Prompt and clarification

Return every distinct subset of an integer array that may contain duplicates. The order of returned subsets does not matter.

> May I sort the input, and should I preserve it?

The interviewer permits a sorted copy. That avoids changing caller-owned data and places equal choices together.

### Derivation

At each depth, choose the next element from a suffix. Equal values at the same depth create identical subset branches, so skip all but the first. Equal values at different depths are still allowed; `[2, 2]` may be valid.

```java
static List<List<Integer>> uniqueSubsets(int[] input) {
    int[] values = input.clone();
    Arrays.sort(values);
    List<List<Integer>> result = new ArrayList<>();
    buildSubsets(values, 0, new ArrayList<>(), result);
    return result;
}

static void buildSubsets(int[] values, int start, List<Integer> path,
                         List<List<Integer>> result) {
    result.add(new ArrayList<>(path));
    for (int i = start; i < values.length; i++) {
        if (i > start && values[i] == values[i - 1]) {
            continue;
        }
        path.add(values[i]);
        buildSubsets(values, i + 1, path, result);
        path.remove(path.size() - 1);
    }
}
```

For `[1, 2, 2]`, the second `2` is skipped only when it competes with the first `2` at the same loop depth. A deeper call may choose it after the first, producing `[2, 2]`.

### Follow-up answers

**Complexity?** Let `R` be the number of distinct subsets. Copying paths costs O(total output elements), bounded by O(nR). Recursion depth is O(n), excluding output.

**Can you avoid sorting?** Track values used at each depth with a set, which adds hashing state and usually makes the proof and output order less simple.

## Round 2: word search in a board

### Prompt

Return whether a word can be formed from horizontally or vertically adjacent cells without reusing a cell in one path.

### Strong candidate plan

The state is `(row, column, wordIndex)`. The invariant is that cells already chosen for the current prefix are marked unavailable. A mismatch or boundary ends the branch. A successful match of the last character ends the search.

```java
static boolean exists(char[][] board, String word) {
    if (word.isEmpty()) {
        return true;
    }
    for (int row = 0; row < board.length; row++) {
        for (int column = 0; column < board[row].length; column++) {
            if (search(board, word, row, column, 0)) {
                return true;
            }
        }
    }
    return false;
}

static boolean search(char[][] board, String word, int row, int column, int index) {
    if (row < 0 || row >= board.length || column < 0
            || column >= board[row].length || board[row][column] != word.charAt(index)) {
        return false;
    }
    if (index == word.length() - 1) {
        return true;
    }
    char saved = board[row][column];
    board[row][column] = '\0';
    boolean found = search(board, word, row + 1, column, index + 1)
            || search(board, word, row - 1, column, index + 1)
            || search(board, word, row, column + 1, index + 1)
            || search(board, word, row, column - 1, index + 1);
    board[row][column] = saved;
    return found;
}
```

### Interviewer follow-ups

**What mutation bug appears with early return?** Returning immediately after a successful neighbor before restoring the cell leaks the marker into the caller's board. Compute the result, restore, then return, or use `try/finally`.

**What about jagged arrays?** Bounds must use `board[row].length` for the current row. A rectangular assumption should be stated if required.

**Complexity?** With `m` cells and word length `L`, a common upper bound is O(m * 3^(L-1)) after the first move because the previous cell cannot be reused, though board shape and repeated characters affect the actual tree. Depth is O(L).

## Round 3: palindrome partitioning

### Prompt

Partition a string into every sequence of palindromic substrings.

### Candidate answer

The decision boundary is the next end index. From `start`, try every substring `[start, end]` that is a palindrome, append it, recurse from `end + 1`, then remove it.

```java
static List<List<String>> palindromePartitions(String text) {
    List<List<String>> result = new ArrayList<>();
    partition(text, 0, new ArrayList<>(), result);
    return result;
}

static void partition(String text, int start, List<String> path,
                      List<List<String>> result) {
    if (start == text.length()) {
        result.add(new ArrayList<>(path));
        return;
    }
    for (int end = start; end < text.length(); end++) {
        if (!isPalindrome(text, start, end)) {
            continue;
        }
        path.add(text.substring(start, end + 1));
        partition(text, end + 1, path, result);
        path.remove(path.size() - 1);
    }
}
```

### Follow-up answers

**Can palindrome checks be improved?** Precompute `palindrome[start][end]` in O(n squared) time and space, or memoize checks. Output enumeration can still be exponential.

**What changes if only the minimum number of cuts is required?** Enumeration is unnecessary. Define a DP state for the minimum cuts from each boundary; that crosses into the Dynamic Programming volume.

**What is a useful test set?** Empty text, one character, no repeated characters, all identical characters, and a case such as `aab` with both short and long palindrome choices.

## Interview closing checklist

State the recursive contract, base case, progress measure, path invariant, restoration rule, safe pruning rule, output cost, maximum depth, and the largest input you would allow on the Java stack.
