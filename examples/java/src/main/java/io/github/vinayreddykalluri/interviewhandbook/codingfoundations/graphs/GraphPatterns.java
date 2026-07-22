// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.graphs;

import java.util.*;

public class GraphPatterns {
    public static List<Integer> bfs(int n, int[] u, int[] v, int start) {
        if (n < 0) throw new IllegalArgumentException("n must be non-negative");
        Objects.requireNonNull(u, "u");
        Objects.requireNonNull(v, "v");
        if (u.length != v.length) throw new IllegalArgumentException("Edge arrays must have equal length");
        if (start < 0 || start >= n) throw new IndexOutOfBoundsException("start must identify a vertex");
        List<List<Integer>> g = new ArrayList<>();
        for (int i = 0; i < n; i++) g.add(new ArrayList<>());
        for (int i = 0; i < u.length; i++) {
            if (u[i] < 0 || u[i] >= n || v[i] < 0 || v[i] >= n) {
                throw new IndexOutOfBoundsException("Edge endpoint outside [0, n)");
            }
            g.get(u[i]).add(v[i]);
            g.get(v[i]).add(u[i]);
        }
        boolean[] vis = new boolean[n];
        List<Integer> order = new ArrayList<>();
        ArrayDeque<Integer> q = new ArrayDeque<>();
        vis[start] = true;
        q.offer(start);
        while (!q.isEmpty()) {
            int x = q.poll();
            order.add(x);
            for (int nxt : g.get(x)) {
                if (!vis[nxt]) {
                    vis[nxt] = true;
                    q.offer(nxt);
                }
            }
        }
        return order;
    }
}
