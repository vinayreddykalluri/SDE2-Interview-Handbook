import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/** Executable mixed-domain assessment for the final Advanced Java revision book. */
public final class AdvancedJavaReadinessAssessment {
    private static int serviceConstructions;

    private AdvancedJavaReadinessAssessment() {}

    public static void main(String[] args) throws Exception {
        verifyInitializationOnDemandHolder();
        verifyRuntimeAnnotationBoundary();
        verifyExhaustiveDomainModel();
        verifyAtomicInvariantTransition();
        verifyAsynchronousFailureBoundary();
        verifyVirtualThreadExecution();
        System.out.println("PASS 6 advanced Java readiness scenarios");
    }

    private static void verifyInitializationOnDemandHolder() {
        check(serviceConstructions == 0, "holder starts uninitialized");
        Service first = ServiceHolder.instance();
        Service second = ServiceHolder.instance();
        check(first == second && serviceConstructions == 1,
                "holder initializes once on active use");
    }

    private static void verifyRuntimeAnnotationBoundary() throws NoSuchMethodException {
        Method method = Handler.class.getDeclaredMethod("handle", String.class);
        InterviewTopic topic = method.getAnnotation(InterviewTopic.class);
        check(topic != null && topic.value().equals("reflection-boundary"),
                "runtime-retained annotation is discoverable");
    }

    private static void verifyExhaustiveDomainModel() {
        Result result = new Failure("timeout");
        check(render(result).equals("failure: timeout"),
                "sealed result handled exhaustively");
    }

    private static String render(Result result) {
        return switch (result) {
            case Success success -> "success: " + success.value();
            case Failure failure -> "failure: " + failure.reason();
        };
    }

    private static void verifyAtomicInvariantTransition() {
        Inventory inventory = new Inventory(5);
        check(inventory.reserve(4), "first reservation");
        check(!inventory.reserve(4), "invariant rejects overdraft");
        check(inventory.available() == 1, "inventory remains non-negative");
    }

    private static void verifyAsynchronousFailureBoundary()
            throws InterruptedException, ExecutionException {
        CompletableFuture<String> stage = CompletableFuture
                .<String>failedFuture(new IllegalStateException("dependency failed"))
                .handle((value, failure) -> failure == null
                        ? value
                        : "fallback:" + failure.getMessage());
        check(stage.get().equals("fallback:dependency failed"),
                "failure converted at explicit stage boundary");
    }

    private static void verifyVirtualThreadExecution()
            throws InterruptedException, ExecutionException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            boolean virtual = executor.submit(
                    () -> Thread.currentThread().isVirtual()).get();
            check(virtual, "virtual-thread task execution");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Service {
        Service() {
            serviceConstructions++;
        }
    }

    private static final class ServiceHolder {
        private static final Service INSTANCE = new Service();

        static Service instance() {
            return INSTANCE;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface InterviewTopic {
        String value();
    }

    private static final class Handler {
        @InterviewTopic("reflection-boundary")
        private void handle(String input) {
            if (input.isEmpty()) {
                throw new IllegalArgumentException("input must not be empty");
            }
        }
    }

    private sealed interface Result permits Success, Failure {}

    private record Success(String value) implements Result {}

    private record Failure(String reason) implements Result {}

    private static final class Inventory {
        private final AtomicReference<State> state;

        Inventory(int available) {
            state = new AtomicReference<>(new State(available, 0));
        }

        boolean reserve(int units) {
            if (units <= 0) {
                throw new IllegalArgumentException("units must be positive");
            }
            while (true) {
                State observed = state.get();
                if (observed.available() < units) {
                    return false;
                }
                State proposed = new State(
                        observed.available() - units,
                        observed.version() + 1);
                if (state.compareAndSet(observed, proposed)) {
                    return true;
                }
            }
        }

        int available() {
            return state.get().available();
        }

        private record State(int available, long version) {
            State {
                if (available < 0) {
                    throw new IllegalArgumentException("negative inventory");
                }
            }
        }
    }
}
