# Essential Recursion and Backtracking Pattern Clinics

Backtracking becomes reliable when every branch is governed by a state contract, a legal-choice rule, and a restoration rule. Balanced-parentheses generation and Sudoku expose those ideas without hiding them behind a large framework.

## Clinic 1: generate balanced parentheses

### State sentence

`build(open, close)` generates every valid completion after placing `open` opening symbols and `close` closing symbols.

Two constraints remove invalid branches before they exist:

- add `(` only while `open < pairs`;
- add `)` only while `close < open`.

The second rule is the prefix invariant: no prefix of a balanced string has more closing than opening symbols. A result is complete when both counts equal `pairs`.

For three pairs, the first depth-first path chooses all opens and then all closes, producing `((()))`. Restoration removes the last character before the sibling choice is explored.

### Complexity language

There are `C_n` valid results, where `C_n` is the nth Catalan number. Materializing each length-`2n` string costs `O(C_n * n)` output work and output space. Call depth and the mutable builder use `O(n)` auxiliary space, excluding returned strings.

## Clinic 2: Sudoku as constraint-state search

### Start with validation

The board must be 9 by 9. Every filled character must be a digit from 1 through 9, and the initial rows, columns, and 3-by-3 boxes must contain no duplicates. Rejecting an invalid starting board is different from reporting that a valid starting board has no completion.

### State and choice

Boolean tables record whether a digit is already used in each row, column, or box. At an empty cell, a digit is legal only when all three entries are false. Choose it, mark all three entries, recurse, then clear all three and restore the cell if the child fails.

The box index is:

```text
(row / 3) * 3 + column / 3
```

An SDE-2 optimization chooses the unfilled cell with the fewest legal digits instead of the first empty cell. That minimum-remaining-values heuristic changes search order, not correctness.

## Runnable Java 21 clinic

```java
import java.util.ArrayList;
import java.util.List;

public final class RecursionCoverageClinic {
    private RecursionCoverageClinic() {
    }

    public static List<String> balancedParentheses(int pairs) {
        if (pairs < 0) {
            throw new IllegalArgumentException("pairs must be nonnegative");
        }
        List<String> answer = new ArrayList<>();
        buildParentheses(pairs, 0, 0, new StringBuilder(), answer);
        return answer;
    }

    private static void buildParentheses(int pairs, int open, int close,
            StringBuilder path, List<String> answer) {
        if (open == pairs && close == pairs) {
            answer.add(path.toString());
            return;
        }
        if (open < pairs) {
            path.append('(');
            buildParentheses(pairs, open + 1, close, path, answer);
            path.deleteCharAt(path.length() - 1);
        }
        if (close < open) {
            path.append(')');
            buildParentheses(pairs, open, close + 1, path, answer);
            path.deleteCharAt(path.length() - 1);
        }
    }

    public static boolean solveSudoku(char[][] board) {
        validateBoardShape(board);
        boolean[][] rowUsed = new boolean[9][10];
        boolean[][] columnUsed = new boolean[9][10];
        boolean[][] boxUsed = new boolean[9][10];

        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                char cell = board[row][column];
                if (cell == '.') {
                    continue;
                }
                if (cell < '1' || cell > '9') {
                    throw new IllegalArgumentException("invalid cell");
                }
                int digit = cell - '0';
                int box = (row / 3) * 3 + column / 3;
                if (rowUsed[row][digit] || columnUsed[column][digit]
                        || boxUsed[box][digit]) {
                    throw new IllegalArgumentException("duplicate starting digit");
                }
                rowUsed[row][digit] = true;
                columnUsed[column][digit] = true;
                boxUsed[box][digit] = true;
            }
        }
        return solveCell(board, 0, rowUsed, columnUsed, boxUsed);
    }

    private static boolean solveCell(char[][] board, int position,
            boolean[][] rowUsed, boolean[][] columnUsed, boolean[][] boxUsed) {
        while (position < 81
                && board[position / 9][position % 9] != '.') {
            position++;
        }
        if (position == 81) {
            return true;
        }

        int row = position / 9;
        int column = position % 9;
        int box = (row / 3) * 3 + column / 3;
        for (int digit = 1; digit <= 9; digit++) {
            if (rowUsed[row][digit] || columnUsed[column][digit]
                    || boxUsed[box][digit]) {
                continue;
            }
            board[row][column] = (char) ('0' + digit);
            rowUsed[row][digit] = true;
            columnUsed[column][digit] = true;
            boxUsed[box][digit] = true;

            if (solveCell(board, position + 1, rowUsed, columnUsed, boxUsed)) {
                return true;
            }

            board[row][column] = '.';
            rowUsed[row][digit] = false;
            columnUsed[column][digit] = false;
            boxUsed[box][digit] = false;
        }
        return false;
    }

    private static void validateBoardShape(char[][] board) {
        if (board == null || board.length != 9) {
            throw new IllegalArgumentException("board must have nine rows");
        }
        for (char[] row : board) {
            if (row == null || row.length != 9) {
                throw new IllegalArgumentException("board must be 9 by 9");
            }
        }
    }

    public static void main(String[] args) {
        List<String> values = balancedParentheses(3);
        assert values.size() == 5 && values.contains("()(())");

        char[][] board = {
            "53..7....".toCharArray(), "6..195...".toCharArray(),
            ".98....6.".toCharArray(), "8...6...3".toCharArray(),
            "4..8.3..1".toCharArray(), "7...2...6".toCharArray(),
            ".6....28.".toCharArray(), "...419..5".toCharArray(),
            "....8..79".toCharArray()
        };
        assert solveSudoku(board);
        assert new String(board[0]).equals("534678912");
        System.out.println("PASS essential recursion clinics");
    }
}
```

Expected output with assertions enabled:

```text
PASS essential recursion clinics
```

## Interviewer follow-up chain with model answers

**Interviewer:** Why not generate all length-`2n` strings and filter them?

**Candidate:** Filtering visits `2^(2n)` leaves. The prefix invariant prevents every invalid prefix from producing descendants, so the search follows only prefixes that can still complete.

**Interviewer:** Is the Sudoku solver guaranteed to run quickly?

**Candidate:** No. Backtracking is exponential in the worst case. Constraint tables make each legality test constant time, and choosing the most constrained cell can reduce the practical tree, but neither changes the worst-case class.

**Interviewer:** How do you make restoration safe if cancellation can throw?

**Candidate:** Put the recursive call inside `try` and restore the cell and three constraint entries in `finally`. Then define whether cancellation returns a partial board or guarantees the original board is restored.
