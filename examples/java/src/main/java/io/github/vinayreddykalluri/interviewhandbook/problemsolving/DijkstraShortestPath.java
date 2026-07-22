// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.problemsolving;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/** Single-source shortest paths for a directed graph with non-negative weights. */
public final class DijkstraShortestPath {
    private DijkstraShortestPath() {
    }

    private record Edge(int to, int weight) {
    }

    private record Candidate(int vertex, long distance) {
    }

    /**
     * Returns minimum distances; unreachable vertices retain {@link Long#MAX_VALUE}.
     * Each edge row is {@code [from, to, weight]}. Time is O((V + E) log V).
     */
    public static long[] shortestPaths(int vertexCount, int[][] edges, int source) {
        if (vertexCount <= 0) {
            throw new IllegalArgumentException("vertexCount must be positive");
        }
        Objects.requireNonNull(edges, "edges");
        requireVertex(source, vertexCount);

        List<List<Edge>> graph = new ArrayList<>(vertexCount);
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            graph.add(new ArrayList<>());
        }
        for (int[] edge : edges) {
            if (edge == null || edge.length != 3) {
                throw new IllegalArgumentException("each edge must contain from, to, and weight");
            }
            requireVertex(edge[0], vertexCount);
            requireVertex(edge[1], vertexCount);
            if (edge[2] < 0) {
                throw new IllegalArgumentException("Dijkstra requires non-negative weights");
            }
            graph.get(edge[0]).add(new Edge(edge[1], edge[2]));
        }

        long[] distance = new long[vertexCount];
        Arrays.fill(distance, Long.MAX_VALUE);
        distance[source] = 0;

        PriorityQueue<Candidate> frontier = new PriorityQueue<>(
                Comparator.comparingLong(Candidate::distance)
        );
        frontier.offer(new Candidate(source, 0));

        while (!frontier.isEmpty()) {
            Candidate current = frontier.poll();
            if (current.distance() != distance[current.vertex()]) {
                continue;
            }
            for (Edge edge : graph.get(current.vertex())) {
                long candidate = Math.addExact(current.distance(), edge.weight());
                if (candidate < distance[edge.to()]) {
                    distance[edge.to()] = candidate;
                    frontier.offer(new Candidate(edge.to(), candidate));
                }
            }
        }
        return distance;
    }

    private static void requireVertex(int vertex, int count) {
        if (vertex < 0 || vertex >= count) {
            throw new IndexOutOfBoundsException("vertex must be in [0, vertexCount)");
        }
    }
}
