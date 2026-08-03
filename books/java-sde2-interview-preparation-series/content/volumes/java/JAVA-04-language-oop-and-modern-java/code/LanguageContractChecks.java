import java.util.ArrayList;
import java.util.List;

/** Executable SDE-2 checks for advanced Java language and API contracts. */
public final class LanguageContractChecks {
    private LanguageContractChecks() {}

    public static void main(String[] args) {
        verifyCompileTimeAndRuntimeSelection();
        verifyValueAndShallowRecordSemantics();
        verifyGenericVariance();
        verifyControlledHeapPollutionFailure();
        verifySealedExhaustiveDispatch();
        verifySuppressedResourceFailure();
        System.out.println("PASS 6 advanced language contract suites");
    }

    private static void verifyCompileTimeAndRuntimeSelection() {
        Parent reference = new Child();
        check(reference.describe().equals("child"), "instance override dispatch");
        check(reference.convert(Integer.valueOf(7)).equals("parent-number"),
                "overload selection uses declared receiver type");
        check(Parent.label().equals("parent-static"), "static method selection");
    }

    private static void verifyValueAndShallowRecordSemantics() {
        Money first = new Money("USD", 500);
        Money second = new Money("USD", 500);
        check(first.equals(second) && first.hashCode() == second.hashCode(),
                "record value equality");

        List<String> mutable = new ArrayList<>(List.of("draft"));
        Snapshot snapshot = new Snapshot(mutable);
        mutable.set(0, "changed");
        check(snapshot.labels().get(0).equals("changed"),
                "record does not deep-freeze components");
    }

    private static void verifyGenericVariance() {
        List<Integer> source = List.of(1, 2, 3);
        List<Number> target = new ArrayList<>();
        copy(source, target);
        check(target.equals(List.of(1, 2, 3)), "producer extends, consumer super");
    }

    private static <T> void copy(List<? extends T> source, List<? super T> target) {
        for (T value : source) {
            target.add(value);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void verifyControlledHeapPollutionFailure() {
        List<String> names = new ArrayList<>();
        List raw = names;
        raw.add(Integer.valueOf(7));

        boolean failedAtCompilerInsertedCast = false;
        try {
            String ignored = names.get(0);
        } catch (ClassCastException expected) {
            failedAtCompilerInsertedCast = true;
        }
        check(failedAtCompilerInsertedCast, "raw write pollutes parameterized list");
    }

    private static void verifySealedExhaustiveDispatch() {
        Expression expression = new Add(new Literal(2), new Literal(5));
        check(evaluate(expression) == 7, "exhaustive pattern switch");
    }

    private static int evaluate(Expression expression) {
        return switch (expression) {
            case Literal literal -> literal.value();
            case Add add -> evaluate(add.left()) + evaluate(add.right());
        };
    }

    private static void verifySuppressedResourceFailure() {
        Throwable observed = null;
        try (FailingResource resource = new FailingResource()) {
            resource.use();
            throw new IllegalArgumentException("body failed");
        } catch (Throwable failure) {
            observed = failure;
        }

        check(observed instanceof IllegalArgumentException, "body failure remains primary");
        check(observed.getSuppressed().length == 1
                        && observed.getSuppressed()[0] instanceof IllegalStateException,
                "close failure is suppressed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static class Parent {
        String describe() {
            return "parent";
        }

        String convert(Number value) {
            return "parent-number";
        }

        static String label() {
            return "parent-static";
        }
    }

    private static final class Child extends Parent {
        @Override
        String describe() {
            return "child";
        }

        String convert(Integer value) {
            return "child-integer";
        }

        static String label() {
            return "child-static";
        }
    }

    private record Money(String currency, long minorUnits) {}

    private record Snapshot(List<String> labels) {}

    private sealed interface Expression permits Literal, Add {}

    private record Literal(int value) implements Expression {}

    private record Add(Expression left, Expression right) implements Expression {}

    private static final class FailingResource implements AutoCloseable {
        void use() {
            // The body intentionally fails after a successful use.
        }

        @Override
        public void close() {
            throw new IllegalStateException("close failed");
        }
    }
}
