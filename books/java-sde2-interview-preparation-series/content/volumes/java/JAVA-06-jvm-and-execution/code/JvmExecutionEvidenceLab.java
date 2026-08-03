import java.util.ArrayList;
import java.util.List;

/** Deterministic evidence checks for JVM execution concepts observable in Java. */
public final class JvmExecutionEvidenceLab {
    private static final List<String> EVENTS = new ArrayList<>();

    private JvmExecutionEvidenceLab() {}

    public static void main(String[] args) {
        verifyInitializationTrigger();
        verifyLoaderBoundary();
        verifyLogicalStackFrames();
        verifyFailedInitializationState();
        verifyReferenceAliasing();
        System.out.println("PASS 5 JVM execution evidence checks");
    }

    private static void verifyInitializationTrigger() {
        int constant = Lazy.COMPILE_TIME_CONSTANT;
        check(constant == 7 && EVENTS.isEmpty(),
                "compile-time constant must not trigger initialization");

        int value = Lazy.runtimeValue;
        check(value == 42 && EVENTS.equals(List.of("Lazy.<clinit>")),
                "active use initializes class exactly once");

        int secondRead = Lazy.runtimeValue;
        check(secondRead == 42 && EVENTS.size() == 1,
                "successful initialization is not repeated");
    }

    private static void verifyLoaderBoundary() {
        check(String.class.getClassLoader() == null,
                "bootstrap-defined String is represented by a null loader");
        check(JvmExecutionEvidenceLab.class.getClassLoader() != null,
                "command-line application class has a defining loader");
    }

    private static void verifyLogicalStackFrames() {
        check(callerLayer(), "StackWalker should observe the logical caller frame");
    }

    private static boolean callerLayer() {
        return stackContains("callerLayer") && calleeLayer();
    }

    private static boolean calleeLayer() {
        return stackContains("calleeLayer");
    }

    private static boolean stackContains(String methodName) {
        return StackWalker.getInstance().walk(frames ->
                frames.anyMatch(frame -> frame.getMethodName().equals(methodName)));
    }

    private static void verifyFailedInitializationState() {
        boolean firstFailure = false;
        try {
            readBrokenValue();
        } catch (ExceptionInInitializerError expected) {
            firstFailure = expected.getCause() instanceof IllegalStateException;
        }
        check(firstFailure, "first failed active use");

        boolean laterFailure = false;
        try {
            readBrokenValue();
        } catch (NoClassDefFoundError expected) {
            laterFailure = true;
        }
        check(laterFailure, "later use of erroneous class");
    }

    private static int readBrokenValue() {
        return Broken.value;
    }

    private static void verifyReferenceAliasing() {
        Payload first = new Payload(9);
        Payload alias = first;
        first = null;
        check(alias.value == 9, "reassigning one reference does not destroy object");
        check(first == null, "first reference was independently reassigned");
    }

    private static int recordInitialization(String label, int value) {
        EVENTS.add(label);
        return value;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Lazy {
        static final int COMPILE_TIME_CONSTANT = 7;
        static int runtimeValue = recordInitialization("Lazy.<clinit>", 42);
    }

    private static final class Broken {
        static int value = failInitialization();

        private static int failInitialization() {
            throw new IllegalStateException("broken initializer");
        }
    }

    private static final class Payload {
        private final int value;

        Payload(int value) {
            this.value = value;
        }
    }
}
