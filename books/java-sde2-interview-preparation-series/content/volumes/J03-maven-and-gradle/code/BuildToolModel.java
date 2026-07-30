import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BuildToolModel {
    private BuildToolModel() {
    }

    public static List<String> topologicalOrder(Map<String, Set<String>> dependencies) {
        Map<String, Integer> remaining = new LinkedHashMap<>();
        Map<String, Set<String>> dependents = new HashMap<>();
        for (String module : dependencies.keySet()) {
            remaining.put(module, 0);
        }
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            for (String dependency : entry.getValue()) {
                if (!dependencies.containsKey(dependency)) {
                    throw new IllegalArgumentException("unknown module: " + dependency);
                }
                remaining.merge(entry.getKey(), 1, Integer::sum);
                dependents.computeIfAbsent(dependency, ignored -> new LinkedHashSet<>())
                        .add(entry.getKey());
            }
        }

        Deque<String> ready = new ArrayDeque<>();
        remaining.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .sorted()
                .forEach(ready::addLast);

        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String module = ready.removeFirst();
            order.add(module);
            List<String> unlocked = new ArrayList<>(
                    dependents.getOrDefault(module, Set.of()));
            Collections.sort(unlocked);
            for (String dependent : unlocked) {
                int count = remaining.merge(dependent, -1, Integer::sum);
                if (count == 0) {
                    ready.addLast(dependent);
                }
            }
        }
        if (order.size() != dependencies.size()) {
            throw new IllegalArgumentException("module graph contains a cycle");
        }
        return List.copyOf(order);
    }

    public static Set<String> affectedModules(
            Map<String, Set<String>> dependencies,
            Set<String> changedModules) {
        Map<String, Set<String>> dependents = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            for (String dependency : entry.getValue()) {
                dependents.computeIfAbsent(dependency, ignored -> new HashSet<>())
                        .add(entry.getKey());
            }
        }
        Set<String> affected = new LinkedHashSet<>(changedModules);
        Deque<String> pending = new ArrayDeque<>(changedModules);
        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            for (String dependent : dependents.getOrDefault(current, Set.of())) {
                if (affected.add(dependent)) {
                    pending.addLast(dependent);
                }
            }
        }
        return Set.copyOf(affected);
    }

    public static String cacheKey(Map<String, String> declaredInputs) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            declaredInputs.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                        digest.update((byte) 0);
                        digest.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
                        digest.update((byte) 0);
                    });
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JDK", exception);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0xf, 16));
            result.append(Character.forDigit(value & 0xf, 16));
        }
        return result.toString();
    }

    public static void main(String[] arguments) {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("domain", Set.of());
        graph.put("storage", Set.of("domain"));
        graph.put("service", Set.of("domain", "storage"));

        List<String> order = topologicalOrder(graph);
        if (!order.equals(List.of("domain", "storage", "service"))) {
            throw new AssertionError("unexpected build order: " + order);
        }
        Set<String> affected = affectedModules(graph, Set.of("domain"));
        if (!affected.equals(Set.of("domain", "storage", "service"))) {
            throw new AssertionError("unexpected affected set: " + affected);
        }

        String first = cacheKey(Map.of("source", "abc", "release", "21"));
        String reordered = cacheKey(Map.of("release", "21", "source", "abc"));
        String changed = cacheKey(Map.of("source", "abcd", "release", "21"));
        if (!first.equals(reordered) || first.equals(changed)) {
            throw new AssertionError("cache-key contract failed");
        }

        try {
            topologicalOrder(Map.of("a", Set.of("b"), "b", Set.of("a")));
            throw new AssertionError("cycle should fail");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        System.out.println("BuildToolModel checks passed");
    }
}
