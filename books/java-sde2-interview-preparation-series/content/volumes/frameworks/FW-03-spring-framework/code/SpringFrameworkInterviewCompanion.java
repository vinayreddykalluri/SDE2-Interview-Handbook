import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * Dependency-free Java 21 companion for Spring Framework interview reasoning.
 *
 * <p>This is not a replacement container. It makes three hidden framework
 * decisions explicit: dependency order, proxy interception, and rollback.
 */
public final class SpringFrameworkInterviewCompanion {
    private SpringFrameworkInterviewCompanion() {
    }

    public static void main(String[] args) {
        validatesDependencyOrderAndCycles();
        validatesCandidateResolution();
        validatesProxyCrossingRules();
        validatesRollbackRules();
        System.out.println("SpringFrameworkInterviewCompanion checks passed");
    }

    private static void validatesDependencyOrderAndCycles() {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        graph.put("orderController", Set.of("orderService"));
        graph.put("orderService", Set.of("orderRepository", "clock"));
        graph.put("orderRepository", Set.of("dataSource"));
        graph.put("clock", Set.of());
        graph.put("dataSource", Set.of());

        List<String> order = creationOrder(graph);
        require(order.indexOf("dataSource") < order.indexOf("orderRepository"),
                "data source must precede repository");
        require(order.indexOf("orderRepository") < order.indexOf("orderService"),
                "repository must precede service");
        require(order.indexOf("orderService") < order.indexOf("orderController"),
                "service must precede controller");

        Map<String, Set<String>> cycle = Map.of(
                "orderService", Set.of("pricingService"),
                "pricingService", Set.of("orderService"));
        expectFailure(() -> creationOrder(cycle), "cycle");
    }

    private static void validatesCandidateResolution() {
        List<Candidate> gateways = List.of(
                new Candidate("stripe", Set.of("online"), true),
                new Candidate("offline", Set.of("offline"), false));

        require(resolve(gateways, "offline").name().equals("offline"),
                "qualifier must win");
        require(resolve(gateways, null).name().equals("stripe"),
                "single primary must win without qualifier");

        expectFailure(() -> resolve(List.of(
                new Candidate("first", Set.of(), false),
                new Candidate("second", Set.of(), false)), null), "ambiguous");
    }

    private static void validatesProxyCrossingRules() {
        MethodShape publicMethod = new MethodShape(true, false, false);
        require(intercepted(CallPath.EXTERNAL_PROXY, ProxyKind.SUBCLASS, publicMethod),
                "external public subclass-proxy call should be intercepted");
        require(!intercepted(CallPath.SELF_INVOCATION, ProxyKind.SUBCLASS, publicMethod),
                "self invocation must bypass proxy advice");
        require(!intercepted(CallPath.EXTERNAL_PROXY, ProxyKind.SUBCLASS,
                        new MethodShape(true, true, false)),
                "final method cannot be overridden by subclass proxy");
        require(!intercepted(CallPath.EXTERNAL_PROXY, ProxyKind.SUBCLASS,
                        new MethodShape(false, false, true)),
                "private method cannot be advised by subclass proxy");
        require(intercepted(CallPath.EXTERNAL_PROXY, ProxyKind.JDK_INTERFACE, publicMethod),
                "interface method reached through JDK proxy should be intercepted");
    }

    private static void validatesRollbackRules() {
        require(outcome(new IllegalStateException("failed"), Set.of())
                        == TransactionOutcome.ROLLBACK,
                "unchecked exception should roll back by default");
        require(outcome(new ImportCheckedException("failed"), Set.of())
                        == TransactionOutcome.COMMIT,
                "checked exception should commit under default rules");
        require(outcome(new ImportCheckedException("failed"),
                        Set.of(ImportCheckedException.class))
                        == TransactionOutcome.ROLLBACK,
                "explicit checked rollback rule should roll back");
    }

    static List<String> creationOrder(Map<String, Set<String>> dependencies) {
        Objects.requireNonNull(dependencies, "dependencies");
        Map<String, Integer> remaining = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            remaining.put(entry.getKey(), entry.getValue().size());
            for (String dependency : entry.getValue()) {
                if (!dependencies.containsKey(dependency)) {
                    throw new IllegalArgumentException(
                            "missing dependency " + dependency + " for " + entry.getKey());
                }
                dependents.computeIfAbsent(dependency, ignored -> new ArrayList<>())
                        .add(entry.getKey());
            }
        }

        Queue<String> ready = new ArrayDeque<>();
        dependencies.keySet().stream()
                .filter(name -> remaining.get(name) == 0)
                .sorted()
                .forEach(ready::add);

        List<String> result = new ArrayList<>();
        while (!ready.isEmpty()) {
            String created = ready.remove();
            result.add(created);
            List<String> next = new ArrayList<>(
                    dependents.getOrDefault(created, List.of()));
            next.sort(String::compareTo);
            for (String dependent : next) {
                int count = remaining.compute(dependent, (key, value) -> value - 1);
                if (count == 0) {
                    ready.add(dependent);
                }
            }
        }

        if (result.size() != dependencies.size()) {
            throw new IllegalArgumentException("dependency cycle detected");
        }
        return List.copyOf(result);
    }

    static Candidate resolve(Collection<Candidate> candidates, String qualifier) {
        List<Candidate> matches = candidates.stream()
                .filter(candidate -> qualifier == null
                        || candidate.qualifiers().contains(qualifier))
                .toList();
        if (matches.size() == 1) {
            return matches.getFirst();
        }
        if (qualifier != null) {
            throw new IllegalArgumentException(
                    matches.isEmpty() ? "no qualified candidate" : "ambiguous qualifier");
        }
        List<Candidate> primary = matches.stream().filter(Candidate::primary).toList();
        if (primary.size() == 1) {
            return primary.getFirst();
        }
        throw new IllegalArgumentException(
                matches.isEmpty() ? "no candidate" : "ambiguous candidates");
    }

    static boolean intercepted(
            CallPath path, ProxyKind kind, MethodShape method) {
        if (path != CallPath.EXTERNAL_PROXY) {
            return false;
        }
        if (kind == ProxyKind.JDK_INTERFACE) {
            return method.publiclyExposed();
        }
        return method.publiclyExposed() && !method.finalMethod() && !method.privateMethod();
    }

    static TransactionOutcome outcome(
            Throwable failure, Set<Class<? extends Throwable>> checkedRollbackRules) {
        if (failure instanceof RuntimeException || failure instanceof Error) {
            return TransactionOutcome.ROLLBACK;
        }
        boolean matched = checkedRollbackRules.stream()
                .anyMatch(type -> type.isAssignableFrom(failure.getClass()));
        return matched ? TransactionOutcome.ROLLBACK : TransactionOutcome.COMMIT;
    }

    private static void expectFailure(Runnable action, String expectedText) {
        try {
            action.run();
            throw new AssertionError("expected failure containing " + expectedText);
        } catch (IllegalArgumentException failure) {
            require(failure.getMessage().contains(expectedText),
                    "unexpected failure: " + failure.getMessage());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record Candidate(String name, Set<String> qualifiers, boolean primary) {
        Candidate {
            Objects.requireNonNull(name, "name");
            qualifiers = Set.copyOf(qualifiers);
        }
    }

    record MethodShape(boolean publiclyExposed, boolean finalMethod, boolean privateMethod) {
    }

    enum CallPath {
        EXTERNAL_PROXY,
        SELF_INVOCATION,
        DIRECT_TARGET
    }

    enum ProxyKind {
        JDK_INTERFACE,
        SUBCLASS
    }

    enum TransactionOutcome {
        COMMIT,
        ROLLBACK
    }

    static final class ImportCheckedException extends Exception {
        private static final long serialVersionUID = 1L;

        ImportCheckedException(String message) {
            super(message);
        }
    }
}
