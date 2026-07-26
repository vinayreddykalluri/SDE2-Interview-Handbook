package com.interviewbook.examples;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GraphAlgorithms {
    private GraphAlgorithms() {}

    public static List<Integer> topologicalOrder(int vertices, int[][] edges) {
        List<List<Integer>> graph = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            graph.add(new ArrayList<>());
        }
        int[] indegree = new int[vertices];
        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
            indegree[edge[1]]++;
        }

        ArrayDeque<Integer> ready = new ArrayDeque<>();
        for (int vertex = 0; vertex < vertices; vertex++) {
            if (indegree[vertex] == 0) {
                ready.addLast(vertex);
            }
        }

        List<Integer> order = new ArrayList<>(vertices);
        while (!ready.isEmpty()) {
            int current = ready.removeFirst();
            order.add(current);
            for (int next : graph.get(current)) {
                if (--indegree[next] == 0) {
                    ready.addLast(next);
                }
            }
        }
        if (order.size() != vertices) {
            throw new IllegalArgumentException("graph contains a cycle");
        }
        return List.copyOf(order);
    }

    public static void verify() {
        List<Integer> order = topologicalOrder(4, new int[][] {{0, 1}, {0, 2}, {1, 3}, {2, 3}});
        int[] position = new int[4];
        for (int i = 0; i < order.size(); i++) {
            position[order.get(i)] = i;
        }
        if (!(position[0] < position[1] && position[0] < position[2]
                && position[1] < position[3] && position[2] < position[3])) {
            throw new AssertionError(Arrays.toString(position));
        }
    }
}
