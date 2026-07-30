import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Dependency-free Java 21 companion for Spring Boot interview reasoning.
 *
 * <p>This is not a Boot replacement. It makes five decisions explicit:
 * configuration precedence, conditional defaults, availability transitions,
 * deadline allocation, and idempotent request claiming.</p>
 */
public final class SpringBootInterviewCompanion {
    private SpringBootInterviewCompanion() {
    }

    public static void main(String[] args) {
        validatesPropertyPrecedenceAndOrigin();
        validatesAutoConfigurationBackOff();
        validatesAvailabilityTransitions();
        validatesDeadlineBudgets();
        validatesIdempotencyClaims();
        System.out.println("SpringBootInterviewCompanion checks passed");
    }

    private static void validatesPropertyPrecedenceAndOrigin() {
        PropertyResolver resolver = new PropertyResolver(List.of(
                new PropertySource("packaged application.yml", 10,
                        Map.of("server.port", "8080", "payment.timeout", "3s")),
                new PropertySource("external production.yml", 20,
                        Map.of("payment.timeout", "2s")),
                new PropertySource("environment", 30,
                        Map.of("server.port", "9090")),
                new PropertySource("command line", 40,
                        Map.of("payment.timeout", "750ms"))));

        ResolvedProperty port = resolver.required("server.port");
        require(port.value().equals("9090"), "environment must override packaged port");
        require(port.origin().equals("environment"), "port origin must be retained");

        ResolvedProperty timeout = resolver.required("payment.timeout");
        require(timeout.value().equals("750ms"), "command line must win");
        require(timeout.origin().equals("command line"), "timeout origin must be retained");
        require(resolver.resolve("missing").isEmpty(), "missing property must remain absent");
    }

    private static void validatesAutoConfigurationBackOff() {
        AutoConfigurationSpec specification = new AutoConfigurationSpec(
                "PaymentClientAutoConfiguration",
                "PaymentClient",
                Set.of("RestClient"),
                "payment.enabled");

        RuntimeInputs defaultInputs = new RuntimeInputs(
                Set.of("RestClient", "PaymentClient"),
                Set.of(),
                Map.of("payment.enabled", "true"));
        ConditionOutcome defaultOutcome = specification.evaluate(defaultInputs);
        require(defaultOutcome.matched(), "default must match when prerequisites exist");
        require(defaultOutcome.action().equals("register PaymentClient"),
                "default must register missing bean");

        RuntimeInputs userOverride = new RuntimeInputs(
                Set.of("RestClient", "PaymentClient"),
                Set.of("PaymentClient"),
                Map.of("payment.enabled", "true"));
        ConditionOutcome overrideOutcome = specification.evaluate(userOverride);
        require(overrideOutcome.matched(), "configuration can match while bean backs off");
        require(overrideOutcome.action().equals("back off for user PaymentClient"),
                "user bean must win");

        RuntimeInputs disabled = new RuntimeInputs(
                Set.of("RestClient", "PaymentClient"),
                Set.of(),
                Map.of("payment.enabled", "false"));
        require(!specification.evaluate(disabled).matched(), "disabled property must skip");

        RuntimeInputs missingClass = new RuntimeInputs(
                Set.of("PaymentClient"),
                Set.of(),
                Map.of("payment.enabled", "true"));
        require(!specification.evaluate(missingClass).matched(),
                "missing class prerequisite must skip");
    }

    private static void validatesAvailabilityTransitions() {
        AvailabilityMachine machine = new AvailabilityMachine();
        require(machine.state() == AvailabilityState.STARTING, "initial state");
        machine.contextRefreshed();
        require(machine.state() == AvailabilityState.STARTED_NOT_READY,
                "refresh does not imply readiness");
        machine.startupWorkCompleted();
        require(machine.acceptsTraffic(), "ready state accepts traffic");
        machine.shutdownRequested();
        require(!machine.acceptsTraffic(), "shutdown refuses new traffic");
        machine.drainCompleted();
        require(machine.state() == AvailabilityState.STOPPED, "drain completes shutdown");

        boolean rejected = false;
        try {
            new AvailabilityMachine().startupWorkCompleted();
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        require(rejected, "invalid readiness transition must fail");
    }

    private static void validatesDeadlineBudgets() {
        DeadlineBudget budget = new DeadlineBudget(Duration.ofSeconds(2));
        budget.allocate("database", Duration.ofMillis(350));
        budget.allocate("payment", Duration.ofMillis(900));
        budget.allocate("response margin", Duration.ofMillis(300));
        require(budget.remaining().equals(Duration.ofMillis(450)), "remaining budget");

        boolean rejected = false;
        try {
            budget.allocate("retry", Duration.ofMillis(600));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "nested work must not exceed caller deadline");
    }

    private static void validatesIdempotencyClaims() {
        IdempotencyStore store = new IdempotencyStore();
        Claim first = store.claim("key-1", "hash-A");
        require(first.status() == ClaimStatus.NEW, "first claim must be new");

        Claim concurrent = store.claim("key-1", "hash-A");
        require(concurrent.status() == ClaimStatus.IN_PROGRESS,
                "same request must observe in-progress state");

        store.complete("key-1", "order-42");
        Claim replay = store.claim("key-1", "hash-A");
        require(replay.status() == ClaimStatus.REPLAY, "completed request must replay");
        require(replay.result().orElseThrow().equals("order-42"), "stored result must return");

        Claim conflict = store.claim("key-1", "hash-B");
        require(conflict.status() == ClaimStatus.CONFLICT,
                "same key with another request must conflict");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record PropertySource(String name, int precedence, Map<String, String> values) {
        PropertySource {
            Objects.requireNonNull(name);
            values = Map.copyOf(values);
        }
    }

    record ResolvedProperty(String name, String value, String origin, int precedence) {
    }

    static final class PropertyResolver {
        private final List<PropertySource> sources;

        PropertyResolver(List<PropertySource> sources) {
            this.sources = new ArrayList<>(sources);
            this.sources.sort(Comparator.comparingInt(PropertySource::precedence).reversed());
        }

        Optional<ResolvedProperty> resolve(String name) {
            return sources.stream()
                    .filter(source -> source.values().containsKey(name))
                    .findFirst()
                    .map(source -> new ResolvedProperty(
                            name,
                            source.values().get(name),
                            source.name(),
                            source.precedence()));
        }

        ResolvedProperty required(String name) {
            return resolve(name).orElseThrow(() ->
                    new IllegalArgumentException("Missing property: " + name));
        }
    }

    record RuntimeInputs(Set<String> classes,
                         Set<String> beans,
                         Map<String, String> properties) {
        RuntimeInputs {
            classes = Set.copyOf(classes);
            beans = Set.copyOf(beans);
            properties = Map.copyOf(properties);
        }
    }

    record ConditionOutcome(boolean matched, List<String> reasons, String action) {
        ConditionOutcome {
            reasons = List.copyOf(reasons);
        }
    }

    record AutoConfigurationSpec(String name,
                                 String beanType,
                                 Set<String> requiredClasses,
                                 String enabledProperty) {
        AutoConfigurationSpec {
            requiredClasses = Set.copyOf(requiredClasses);
        }

        ConditionOutcome evaluate(RuntimeInputs inputs) {
            List<String> reasons = new ArrayList<>();
            Set<String> missing = new java.util.LinkedHashSet<>(requiredClasses);
            missing.removeAll(inputs.classes());
            if (!missing.isEmpty()) {
                reasons.add("missing classes " + missing);
                return new ConditionOutcome(false, reasons, "skip " + name);
            }
            reasons.add("required classes present");

            boolean enabled = Boolean.parseBoolean(
                    inputs.properties().getOrDefault(enabledProperty, "true"));
            if (!enabled) {
                reasons.add(enabledProperty + " is false");
                return new ConditionOutcome(false, reasons, "skip " + name);
            }
            reasons.add(enabledProperty + " is true");

            if (inputs.beans().contains(beanType)) {
                reasons.add("user bean exists");
                return new ConditionOutcome(true, reasons, "back off for user " + beanType);
            }
            reasons.add("bean is missing");
            return new ConditionOutcome(true, reasons, "register " + beanType);
        }
    }

    enum AvailabilityState {
        STARTING,
        STARTED_NOT_READY,
        ACCEPTING_TRAFFIC,
        DRAINING,
        STOPPED
    }

    static final class AvailabilityMachine {
        private AvailabilityState state = AvailabilityState.STARTING;

        AvailabilityState state() {
            return state;
        }

        boolean acceptsTraffic() {
            return state == AvailabilityState.ACCEPTING_TRAFFIC;
        }

        void contextRefreshed() {
            transition(AvailabilityState.STARTING, AvailabilityState.STARTED_NOT_READY);
        }

        void startupWorkCompleted() {
            transition(AvailabilityState.STARTED_NOT_READY, AvailabilityState.ACCEPTING_TRAFFIC);
        }

        void shutdownRequested() {
            transition(AvailabilityState.ACCEPTING_TRAFFIC, AvailabilityState.DRAINING);
        }

        void drainCompleted() {
            transition(AvailabilityState.DRAINING, AvailabilityState.STOPPED);
        }

        private void transition(AvailabilityState expected, AvailabilityState next) {
            if (state != expected) {
                throw new IllegalStateException(
                        "Expected " + expected + " but was " + state);
            }
            state = next;
        }
    }

    static final class DeadlineBudget {
        private final Duration total;
        private final Map<String, Duration> allocations = new LinkedHashMap<>();

        DeadlineBudget(Duration total) {
            if (total.isNegative() || total.isZero()) {
                throw new IllegalArgumentException("total must be positive");
            }
            this.total = total;
        }

        void allocate(String name, Duration duration) {
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("allocation must be positive");
            }
            if (duration.compareTo(remaining()) > 0) {
                throw new IllegalArgumentException("allocation exceeds remaining deadline");
            }
            allocations.put(name, duration);
        }

        Duration remaining() {
            Duration used = allocations.values().stream()
                    .reduce(Duration.ZERO, Duration::plus);
            return total.minus(used);
        }
    }

    enum ClaimStatus {
        NEW,
        IN_PROGRESS,
        REPLAY,
        CONFLICT
    }

    record Claim(ClaimStatus status, Optional<String> result) {
        static Claim of(ClaimStatus status) {
            return new Claim(status, Optional.empty());
        }
    }

    record StoredRequest(String requestHash, Optional<String> result) {
        StoredRequest {
            Objects.requireNonNull(requestHash);
            Objects.requireNonNull(result);
        }
    }

    static final class IdempotencyStore {
        private final Map<String, StoredRequest> requests = new HashMap<>();

        Claim claim(String key, String requestHash) {
            StoredRequest existing = requests.get(key);
            if (existing == null) {
                requests.put(key, new StoredRequest(requestHash, Optional.empty()));
                return Claim.of(ClaimStatus.NEW);
            }
            if (!existing.requestHash().equals(requestHash)) {
                return Claim.of(ClaimStatus.CONFLICT);
            }
            return existing.result()
                    .map(result -> new Claim(ClaimStatus.REPLAY, Optional.of(result)))
                    .orElseGet(() -> Claim.of(ClaimStatus.IN_PROGRESS));
        }

        void complete(String key, String result) {
            StoredRequest existing = Optional.ofNullable(requests.get(key))
                    .orElseThrow(() -> new IllegalArgumentException("unknown key"));
            requests.put(key, new StoredRequest(
                    existing.requestHash(), Optional.of(result)));
        }
    }
}
