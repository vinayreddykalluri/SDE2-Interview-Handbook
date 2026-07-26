# Grid Indexes and Traversals

A two-dimensional traversal is not “just two loops.” It combines a data-layout contract, two independent bounds, and an order of visitation. Java adds an important detail: `int[][]` is an array of row references, so rows may be null or have different lengths.

## 5.1 What Java stores

```java
int[][] matrix = {
    {1, 2, 3},
    {4, 5, 6}
};
```

`matrix.length` is the number of row references: 2. `matrix[row].length` is the length of one row: 3 here. Java does not require every row to have the same length:

```java
int[][] ragged = {
    {1, 2},
    {3},
    {},
    {4, 5, 6}
};
```

The safe general traversal uses the current row's length:

```java
for (int row = 0; row < ragged.length; row++) {
    if (ragged[row] == null) {
        continue; // or reject null rows by contract
    }
    for (int col = 0; col < ragged[row].length; col++) {
        process(ragged[row][col]);
    }
}
```

If an algorithm requires a rectangle, validate it once at the boundary and then use the proven column count.

## 5.2 Rectangular validation

```java
static int requireRectangular(int[][] matrix) {
    if (matrix == null) {
        throw new IllegalArgumentException("matrix must not be null");
    }
    if (matrix.length == 0) {
        return 0;
    }
    if (matrix[0] == null) {
        throw new IllegalArgumentException("row must not be null");
    }
    int columns = matrix[0].length;
    for (int[] row : matrix) {
        if (row == null || row.length != columns) {
            throw new IllegalArgumentException("matrix must be rectangular");
        }
    }
    return columns;
}
```

Decide how to treat a nonempty zero-column matrix such as `new int[3][0]`. It is a valid Java value. Most traversals can accept it and visit zero cells.

## 5.3 Row-major traversal

```java
int columns = requireRectangular(matrix);
for (int row = 0; row < matrix.length; row++) {
    for (int col = 0; col < columns; col++) {
        process(matrix[row][col]);
    }
}
```

The outer loop fixes a row; the inner loop visits its columns. Invariant at the start of row `r`: all cells in rows `[0, r)` have been processed. Within a row, columns `[0, c)` have been processed.

Time is `O(rows * columns)`. Working traversal state is `O(1)`. If values are collected, report output space separately.

Row-major order usually has better locality for Java arrays because elements of one row occupy a contiguous array. This is a useful performance intuition, not a promise about an exact cache layout or speed on every JVM.

## 5.4 Column-major traversal

```java
for (int col = 0; col < columns; col++) {
    for (int row = 0; row < matrix.length; row++) {
        process(matrix[row][col]);
    }
}
```

The same cells are visited in a different order. Column-major traversal requires rectangular rows or a defined policy for missing cells. Choose it when the problem is column-oriented, not merely because swapping loops looks symmetrical.

## 5.5 Flattening a grid index

For a rectangular row-major grid:

```text
flat = row * columns + col
row  = flat / columns
col  = flat % columns
```

![Mapping row and column coordinates to a row-major flat index](content/volumes/05-loop-mastery-and-index-calculations/assets/07-flatten-unflatten-grid.png)

### Derivation

Every complete preceding row contributes `columns` cells. Before `(row, col)`, there are `row * columns` cells in previous rows and `col` cells in the current row.

For a 3 by 4 grid, `(2,1)` becomes `2 * 4 + 1 = 9`. Unflattening 9 gives `9 / 4 = 2` and `9 % 4 = 1`.

### Safe implementation

```java
record Cell(int row, int col) {}

static long flatten(int row, int col, int rows, int columns) {
    if (rows <= 0 || columns <= 0) {
        throw new IllegalArgumentException("dimensions must be positive");
    }
    if (row < 0 || row >= rows || col < 0 || col >= columns) {
        throw new IndexOutOfBoundsException("cell outside grid");
    }
    return Math.addExact(Math.multiplyExact((long) row, columns), col);
}

static Cell unflatten(long flat, int rows, int columns) {
    if (rows <= 0 || columns <= 0) {
        throw new IllegalArgumentException("dimensions must be positive");
    }
    long capacity = Math.multiplyExact((long) rows, columns);
    if (flat < 0 || flat >= capacity) {
        throw new IndexOutOfBoundsException("index outside grid");
    }
    return new Cell((int) (flat / columns), (int) (flat % columns));
}
```

The layout formula does not work for a ragged array because no single column count describes all row starts. A ragged layout needs prefix row offsets or row-by-row storage metadata.

## 5.6 Neighbor calculations

For four-direction movement:

```java
int[] deltaRow = {-1, 0, 1, 0};
int[] deltaCol = {0, 1, 0, -1};

for (int direction = 0; direction < 4; direction++) {
    int nextRow = row + deltaRow[direction];
    int nextCol = col + deltaCol[direction];
    if (0 <= nextRow && nextRow < rows
            && 0 <= nextCol && nextCol < columns) {
        process(matrix[nextRow][nextCol]);
    }
}
```

The two direction arrays must stay aligned. An alternative is one array of coordinate pairs. Compute the candidate before accessing it, then validate both dimensions.

For eight-direction neighbors, include diagonal deltas and normally skip `(0,0)`. Clarify whether diagonal movement is permitted and whether corner cutting is allowed in pathfinding.

## 5.7 Diagonals

The main diagonal is `(i, i)`. In a rectangular matrix its length is `min(rows, columns)`:

```java
for (int i = 0; i < Math.min(rows, columns); i++) {
    process(matrix[i][i]);
}
```

The anti-diagonal beginning at top-right is `(i, columns - 1 - i)` under the same bound.

Cells on the same descending diagonal share `row - col`; cells on the same ascending diagonal share `row + col`. For positive dimensions, `row + col` ranges from 0 through `rows + columns - 2`, so there are `rows + columns - 1` such groups.

Watch overflow if dimensions or derived keys are unconstrained. Ordinary Java arrays cannot have `Integer.MAX_VALUE` practical dimensions, but production APIs may accept abstract coordinates not backed by one in-memory array.

## 5.8 Spiral traversal as a shrinking rectangle

Maintain inclusive boundaries:

```text
top <= bottom
left <= right
```

Visit the top edge, right edge, bottom edge if still present, and left edge if still present. Then repeat on the inner rectangle.

![Spiral traversal as processed rings and a shrinking unvisited rectangle](content/volumes/05-loop-mastery-and-index-calculations/assets/08-spiral-boundaries.png)

```java
static java.util.List<Integer> spiral(int[][] matrix) {
    int columns = requireRectangular(matrix);
    java.util.List<Integer> result = new java.util.ArrayList<>();
    if (matrix.length == 0 || columns == 0) return result;

    int top = 0;
    int bottom = matrix.length - 1;
    int left = 0;
    int right = columns - 1;

    while (top <= bottom && left <= right) {
        for (int col = left; col <= right; col++) {
            result.add(matrix[top][col]);
        }
        top++;

        for (int row = top; row <= bottom; row++) {
            result.add(matrix[row][right]);
        }
        right--;

        if (top <= bottom) {
            for (int col = right; col >= left; col--) {
                result.add(matrix[bottom][col]);
            }
            bottom--;
        }

        if (left <= right) {
            for (int row = bottom; row >= top; row--) {
                result.add(matrix[row][left]);
            }
            left++;
        }
    }
    return result;
}
```

### Why the guards matter

After consuming the top edge, a one-row rectangle has no distinct bottom edge. After consuming the right edge, a one-column rectangle has no distinct left edge. The guards prevent duplicate visits.

Invariant: all cells outside `[top, bottom] x [left, right]` have been emitted exactly once; all cells inside remain unvisited.

Each cell is emitted once, so time is `O(rows * columns)`. Boundary updates use `O(1)` state, excluding the returned list.

## 5.9 Ring traversal and rotation

For layer `layer`, the boundaries are:

```text
top = layer
left = layer
bottom = rows - 1 - layer
right = columns - 1 - layer
```

The number of complete layers is `(min(rows, columns) + 1) / 2` for traversal, but a rotatable ring usually requires at least two rows and two columns. Define behavior for one-row and one-column layers.

Ring rotation is mutation-sensitive: save one corner, shift edges in an overwrite-safe order, and test 2x2 before larger shapes. The Arrays volume develops rotations in depth.

## 5.10 Flat iteration without nested loops

For a validated rectangular grid, every flat index in `[0, rows * columns)` maps to a cell:

```java
long total = Math.multiplyExact((long) rows, columns);
for (long flat = 0; flat < total; flat++) {
    int row = (int) (flat / columns);
    int col = (int) (flat % columns);
    process(matrix[row][col]);
}
```

This is still `O(rows * columns)` work. A single visible loop does not make it `O(rows + columns)`. Complexity follows visit count, not indentation.

## 5.11 Common failures

1. Using `matrix[0].length` for every row without validating rectangularity.
2. Accessing `matrix[0]` before checking `matrix.length == 0`.
3. Treating a null row as an empty row without an explicit contract.
4. Reversing row and column bounds.
5. Flattening with `int` multiplication before widening.
6. Unflattening without checking total capacity.
7. Duplicating the final spiral row or column.
8. Assuming a diagonal requires a square matrix.
9. Accessing a neighbor before validating it.
10. Forgetting that stored flat indexes require the same column-count metadata later.

## 5.12 Production considerations

- Validate dimensions before allocating `rows * columns` storage.
- Persist layout version and dimensions with flattened data.
- Avoid materializing traversal output when a visitor or iterator can stream it.
- Define mutation ownership for in-place transforms.
- Cap requested output size for service endpoints.
- Use tile/block traversal only after profiling; it changes implementation complexity.
- For sparse grids, adjacency maps or compressed structures may be more suitable than dense matrices.

## 5.13 Interview checkpoint

You should be able to distinguish rectangular from ragged arrays, derive flatten/unflatten formulas, use checked wide arithmetic, traverse rows, columns, diagonals, neighbors, and spirals, prove every cell is visited once, and test empty, one-row, one-column, and non-square matrices.
