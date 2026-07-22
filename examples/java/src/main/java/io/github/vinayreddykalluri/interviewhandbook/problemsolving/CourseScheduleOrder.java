// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.problemsolving;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Kahn's topological-sort algorithm with explicit cycle detection. */
public final class CourseScheduleOrder {
    private CourseScheduleOrder() {
    }

    /**
     * Returns a valid course order, or an empty array when prerequisites contain a cycle.
     * Each row is {@code [course, prerequisite]}. Time is O(V + E); space is O(V + E).
     */
    public static int[] order(int courseCount, int[][] prerequisites) {
        if (courseCount < 0) {
            throw new IllegalArgumentException("courseCount must be non-negative");
        }
        Objects.requireNonNull(prerequisites, "prerequisites");

        List<List<Integer>> dependents = new ArrayList<>(courseCount);
        for (int course = 0; course < courseCount; course++) {
            dependents.add(new ArrayList<>());
        }

        int[] indegree = new int[courseCount];
        for (int[] relation : prerequisites) {
            if (relation == null || relation.length != 2) {
                throw new IllegalArgumentException("each prerequisite must contain two vertices");
            }
            int course = relation[0];
            int prerequisite = relation[1];
            requireVertex(course, courseCount);
            requireVertex(prerequisite, courseCount);
            dependents.get(prerequisite).add(course);
            indegree[course]++;
        }

        ArrayDeque<Integer> ready = new ArrayDeque<>();
        for (int course = 0; course < courseCount; course++) {
            if (indegree[course] == 0) {
                ready.offer(course);
            }
        }

        int[] result = new int[courseCount];
        int written = 0;
        while (!ready.isEmpty()) {
            int completed = ready.poll();
            result[written++] = completed;
            for (int dependent : dependents.get(completed)) {
                if (--indegree[dependent] == 0) {
                    ready.offer(dependent);
                }
            }
        }
        return written == courseCount ? result : new int[0];
    }

    private static void requireVertex(int vertex, int count) {
        if (vertex < 0 || vertex >= count) {
            throw new IndexOutOfBoundsException("vertex must be in [0, courseCount)");
        }
    }
}
