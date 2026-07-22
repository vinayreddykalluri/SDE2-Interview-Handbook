// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.problemsolving;

/** Union-find with path compression and union by component size. */
public final class DisjointSet {
    private final int[] parent;
    private final int[] componentSize;
    private int componentCount;

    public DisjointSet(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        parent = new int[size];
        componentSize = new int[size];
        componentCount = size;
        for (int index = 0; index < size; index++) {
            parent[index] = index;
            componentSize[index] = 1;
        }
    }

    /** Finds the representative with amortized inverse-Ackermann cost. */
    public int find(int value) {
        requireElement(value);
        int root = value;
        while (root != parent[root]) {
            root = parent[root];
        }
        while (value != root) {
            int next = parent[value];
            parent[value] = root;
            value = next;
        }
        return root;
    }

    /** Returns true only when two previously separate components were merged. */
    public boolean union(int first, int second) {
        int firstRoot = find(first);
        int secondRoot = find(second);
        if (firstRoot == secondRoot) {
            return false;
        }
        if (componentSize[firstRoot] < componentSize[secondRoot]) {
            int temporary = firstRoot;
            firstRoot = secondRoot;
            secondRoot = temporary;
        }
        parent[secondRoot] = firstRoot;
        componentSize[firstRoot] += componentSize[secondRoot];
        componentCount--;
        return true;
    }

    public boolean connected(int first, int second) {
        return find(first) == find(second);
    }

    public int componentSize(int value) {
        return componentSize[find(value)];
    }

    public int componentCount() {
        return componentCount;
    }

    private void requireElement(int value) {
        if (value < 0 || value >= parent.length) {
            throw new IndexOutOfBoundsException("element must be in [0, size)");
        }
    }
}
