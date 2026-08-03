import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GitBookJavaFixture {
    private GitBookJavaFixture() {
    }

    record PullRequest(String title, int priority) {
    }

    static List<PullRequest> orderSafely(List<PullRequest> requests) {
        List<PullRequest> ordered = new ArrayList<>(requests);
        ordered.sort(Comparator.comparingInt(PullRequest::priority)
                .thenComparing(PullRequest::title));
        return List.copyOf(ordered);
    }

    static Set<String> affectedModules(List<String> changedPaths) {
        Set<String> affected = new LinkedHashSet<>();
        for (String path : changedPaths) {
            if (path.equals("pom.xml")
                    || path.equals("settings.gradle.kts")
                    || path.startsWith("build-logic/")) {
                return Set.of("api", "domain", "persistence");
            }
            if (path.startsWith("api/")) {
                affected.add("api");
            } else if (path.startsWith("domain/")) {
                affected.add("domain");
                affected.add("api");
            } else if (path.startsWith("persistence/")) {
                affected.add("persistence");
                affected.add("api");
            }
        }
        return Set.copyOf(affected);
    }

    public static void main(String[] args) {
        List<PullRequest> ordered = orderSafely(List.of(
                new PullRequest("Maximum", Integer.MAX_VALUE),
                new PullRequest("Minimum", Integer.MIN_VALUE),
                new PullRequest("Normal", 10)));
        assert ordered.get(0).title().equals("Minimum");
        assert ordered.get(2).title().equals("Maximum");

        Set<String> leafChange = affectedModules(List.of(
                "domain/src/main/java/example/Order.java"));
        assert leafChange.equals(Set.of("domain", "api"));

        Set<String> rootBuildChange = affectedModules(List.of("pom.xml"));
        assert rootBuildChange.equals(Set.of("api", "domain", "persistence"));

        System.out.println("GitBookJavaFixture: all checks passed");
    }
}
