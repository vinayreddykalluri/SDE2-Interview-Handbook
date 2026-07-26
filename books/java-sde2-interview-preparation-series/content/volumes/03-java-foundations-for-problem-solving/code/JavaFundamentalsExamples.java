import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

/**
 * Seventy dependency-free Java 21 fundamentals examples.
 *
 * Each example is a small executable check. A successful run prints exactly:
 * PASS 70 Java Fundamentals examples
 */
public final class JavaFundamentalsExamples {
    private JavaFundamentalsExamples() {}

    public static void main(String[] args) {
        List<Runnable> examples = List.of(
                JavaFundamentalsExamples::example01FirstProgram,
                JavaFundamentalsExamples::example02VariableDeclaration,
                JavaFundamentalsExamples::example03PrimitiveRanges,
                JavaFundamentalsExamples::example04NumericPromotion,
                JavaFundamentalsExamples::example05SafeLongMultiplication,
                JavaFundamentalsExamples::example06WideningConversion,
                JavaFundamentalsExamples::example07NarrowingConversion,
                JavaFundamentalsExamples::example08IntegerDivision,
                JavaFundamentalsExamples::example09ShortCircuitEvaluation,
                JavaFundamentalsExamples::example10PrefixAndPostfix,
                JavaFundamentalsExamples::example11ConditionalStatements,
                JavaFundamentalsExamples::example12TraditionalSwitch,
                JavaFundamentalsExamples::example13SwitchExpression,
                JavaFundamentalsExamples::example14ForwardLoop,
                JavaFundamentalsExamples::example15ReverseLoop,
                JavaFundamentalsExamples::example16EnhancedForLoop,
                JavaFundamentalsExamples::example17NestedLoop,
                JavaFundamentalsExamples::example18MethodParametersAndReturn,
                JavaFundamentalsExamples::example19MethodOverloading,
                JavaFundamentalsExamples::example20PrimitivePassByValue,
                JavaFundamentalsExamples::example21MutableObjectPassByValue,
                JavaFundamentalsExamples::example22ReferenceReassignment,
                JavaFundamentalsExamples::example23OneDimensionalArray,
                JavaFundamentalsExamples::example24TwoDimensionalArray,
                JavaFundamentalsExamples::example25JaggedArray,
                JavaFundamentalsExamples::example26ArrayAliasing,
                JavaFundamentalsExamples::example27ArrayCopy,
                JavaFundamentalsExamples::example28StringEquality,
                JavaFundamentalsExamples::example29StringPool,
                JavaFundamentalsExamples::example30StringBuilder,
                JavaFundamentalsExamples::example31CharacterToDigit,
                JavaFundamentalsExamples::example32ClassAndObject,
                JavaFundamentalsExamples::example33ConstructorOverloading,
                JavaFundamentalsExamples::example34ConstructorChaining,
                JavaFundamentalsExamples::example35StaticFieldAndMethod,
                JavaFundamentalsExamples::example36Encapsulation,
                JavaFundamentalsExamples::example37ImmutableClass,
                JavaFundamentalsExamples::example38Inheritance,
                JavaFundamentalsExamples::example39MethodOverriding,
                JavaFundamentalsExamples::example40RuntimePolymorphism,
                JavaFundamentalsExamples::example41AbstractClass,
                JavaFundamentalsExamples::example42Interface,
                JavaFundamentalsExamples::example43Composition,
                JavaFundamentalsExamples::example44WrapperParsing,
                JavaFundamentalsExamples::example45AutoboxingAndUnboxing,
                JavaFundamentalsExamples::example46IntegerCaching,
                JavaFundamentalsExamples::example47GenericClassAndMethod,
                JavaFundamentalsExamples::example48ArrayList,
                JavaFundamentalsExamples::example49HashSet,
                JavaFundamentalsExamples::example50HashMapFrequency,
                JavaFundamentalsExamples::example51QueueWithArrayDeque,
                JavaFundamentalsExamples::example52StackWithArrayDeque,
                JavaFundamentalsExamples::example53PriorityQueue,
                JavaFundamentalsExamples::example54Enum,
                JavaFundamentalsExamples::example55CheckedException,
                JavaFundamentalsExamples::example56UncheckedException,
                JavaFundamentalsExamples::example57TryWithResources,
                JavaFundamentalsExamples::example58ScannerInput,
                JavaFundamentalsExamples::example59BufferedReaderInput,
                JavaFundamentalsExamples::example60InterviewQualityRefactoring,
                JavaFundamentalsExamples::example61ListOperations,
                JavaFundamentalsExamples::example62ListRemovalOverloads,
                JavaFundamentalsExamples::example63SetVariants,
                JavaFundamentalsExamples::example64MapUpdateApis,
                JavaFundamentalsExamples::example65MapIteration,
                JavaFundamentalsExamples::example66DequeConventions,
                JavaFundamentalsExamples::example67PriorityQueueComparator,
                JavaFundamentalsExamples::example68CollectionFactories,
                JavaFundamentalsExamples::example69SafeIteratorRemoval,
                JavaFundamentalsExamples::example70CopiesAndViews);

        examples.forEach(Runnable::run);
        System.out.println("PASS " + examples.size() + " Java Fundamentals examples");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void example01FirstProgram() {
        String output = "Ready for Java";
        check(output.equals("Ready for Java"), "first program");
    }

    static void example02VariableDeclaration() {
        int solvedProblems = 10;
        solvedProblems = 11;
        check(solvedProblems == 11, "assignment");
    }

    static void example03PrimitiveRanges() {
        check(Byte.MIN_VALUE == -128 && Byte.MAX_VALUE == 127, "byte range");
        check(Integer.MIN_VALUE == -2_147_483_648, "int range");
    }

    static void example04NumericPromotion() {
        byte left = 100;
        byte right = 20;
        int sum = left + right;
        check(sum == 120, "byte operands promote to int");
    }

    static void example05SafeLongMultiplication() {
        long product = 100_000L * 100_000;
        check(product == 10_000_000_000L, "widen before multiply");
    }

    static void example06WideningConversion() {
        int count = 10;
        long total = count;
        check(total == 10L, "widening");
    }

    static void example07NarrowingConversion() {
        double value = 10.8;
        int truncated = (int) value;
        check(truncated == 10, "narrowing truncates");
    }

    static void example08IntegerDivision() {
        check(5 / 2 == 2, "integer division");
        check(5 / 2.0 == 2.5, "floating division");
    }

    static void example09ShortCircuitEvaluation() {
        String text = null;
        boolean present = text != null && !text.isEmpty();
        check(!present, "short circuit null guard");
    }

    static void example10PrefixAndPostfix() {
        int value = 5;
        int before = value++;
        int after = ++value;
        check(value == 7 && before == 5 && after == 7, "increments");
    }

    static void example11ConditionalStatements() {
        int score = 72;
        String grade;
        if (score >= 70) grade = "pass";
        else grade = "retry";
        check(grade.equals("pass"), "if else");
    }

    static String traditionalSwitch(int day) {
        String kind;
        switch (day) {
            case 6:
            case 7:
                kind = "weekend";
                break;
            default:
                kind = "weekday";
        }
        return kind;
    }

    static void example12TraditionalSwitch() {
        check(traditionalSwitch(7).equals("weekend"), "traditional switch");
    }

    static String switchExpression(int day) {
        return switch (day) {
            case 6, 7 -> "weekend";
            default -> "weekday";
        };
    }

    static void example13SwitchExpression() {
        check(switchExpression(1).equals("weekday"), "switch expression");
    }

    static void example14ForwardLoop() {
        int sum = 0;
        for (int value = 1; value <= 3; value++) sum += value;
        check(sum == 6, "forward loop");
    }

    static void example15ReverseLoop() {
        StringBuilder order = new StringBuilder();
        for (int index = 2; index >= 0; index--) order.append(index);
        check(order.toString().equals("210"), "reverse loop");
    }

    static void example16EnhancedForLoop() {
        int sum = 0;
        for (int number : new int[] {2, 4, 6}) sum += number;
        check(sum == 12, "enhanced for");
    }

    static void example17NestedLoop() {
        int cells = 0;
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) cells++;
        }
        check(cells == 6, "nested loop");
    }

    static int add(int left, int right) {
        return left + right;
    }

    static void example18MethodParametersAndReturn() {
        check(add(4, 5) == 9, "method result");
    }

    static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    static long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    static void example19MethodOverloading() {
        check(clamp(12, 0, 10) == 10, "int overload");
        check(clamp(12L, 0L, 10L) == 10L, "long overload");
    }

    static void increment(int number) {
        number++;
        check(number == 8, "local primitive changed");
    }

    static void example20PrimitivePassByValue() {
        int caller = 7;
        increment(caller);
        check(caller == 7, "caller primitive unchanged");
    }

    static final class Person {
        String name;
        Person(String name) { this.name = name; }
    }

    static void rename(Person person) {
        person.name = "Updated";
    }

    static void example21MutableObjectPassByValue() {
        Person person = new Person("Original");
        rename(person);
        check(person.name.equals("Updated"), "shared mutation");
    }

    static void replace(Person person) {
        person = new Person("New");
        check(person.name.equals("New"), "local reference reassigned");
    }

    static void example22ReferenceReassignment() {
        Person person = new Person("Caller");
        replace(person);
        check(person.name.equals("Caller"), "caller reference unchanged");
    }

    static void example23OneDimensionalArray() {
        int[] values = {2, 4, 6};
        check(values.length == 3 && values[1] == 4, "one dimensional array");
    }

    static void example24TwoDimensionalArray() {
        int[][] matrix = {{1, 2}, {3, 4}};
        check(matrix[1][0] == 3, "matrix");
    }

    static void example25JaggedArray() {
        int[][] rows = {new int[1], new int[3]};
        check(rows[0].length == 1 && rows[1].length == 3, "jagged array");
    }

    static void example26ArrayAliasing() {
        int[] first = {1, 2};
        int[] alias = first;
        alias[0] = 9;
        check(first[0] == 9, "array aliases share mutation");
    }

    static void example27ArrayCopy() {
        int[] original = {1, 2};
        int[] copy = Arrays.copyOf(original, original.length);
        copy[0] = 9;
        check(original[0] == 1, "independent primitive array copy");
    }

    static void example28StringEquality() {
        String left = new String("java");
        String right = new String("java");
        check(left != right && left.equals(right), "identity versus content");
    }

    static void example29StringPool() {
        String first = "pool";
        String second = "pool";
        String explicit = new String("pool");
        check(first == second && first != explicit, "pool and explicit construction");
    }

    static void example30StringBuilder() {
        String result = new StringBuilder().append("java").append('-').append(21).toString();
        check(result.equals("java-21"), "builder");
    }

    static void example31CharacterToDigit() {
        char character = '7';
        int asciiDigit = character - '0';
        int generalDigit = Character.digit(character, 10);
        check(asciiDigit == 7 && generalDigit == 7, "digit conversion");
    }

    static final class Student {
        final int id;
        String name;
        Student(int id, String name) { this.id = id; this.name = name; }
    }

    static void example32ClassAndObject() {
        Student student = new Student(1, "Ada");
        check(student.id == 1 && student.name.equals("Ada"), "class and object");
    }

    static final class Rectangle {
        final int width;
        final int height;
        Rectangle() { this(1, 1); }
        Rectangle(int width, int height) { this.width = width; this.height = height; }
        int area() { return width * height; }
    }

    static void example33ConstructorOverloading() {
        check(new Rectangle().area() == 1, "no argument constructor");
        check(new Rectangle(3, 4).area() == 12, "parameter constructor");
    }

    static void example34ConstructorChaining() {
        Rectangle unit = new Rectangle();
        check(unit.width == 1 && unit.height == 1, "this constructor chaining");
    }

    static final class Counter {
        private static int created;
        Counter() { created++; }
        static int created() { return created; }
    }

    static void example35StaticFieldAndMethod() {
        int before = Counter.created();
        new Counter();
        new Counter();
        check(Counter.created() == before + 2, "static shared count");
    }

    static final class BoundedCounter {
        private final int maximum;
        private int value;
        BoundedCounter(int maximum) { this.maximum = maximum; }
        void increment() { if (value == maximum) throw new IllegalStateException(); value++; }
        int value() { return value; }
    }

    static void example36Encapsulation() {
        BoundedCounter counter = new BoundedCounter(2);
        counter.increment();
        check(counter.value() == 1, "encapsulated invariant");
    }

    static final class Coordinate {
        private final int row;
        private final int column;
        Coordinate(int row, int column) { this.row = row; this.column = column; }
        int row() { return row; }
        int column() { return column; }
    }

    static void example37ImmutableClass() {
        Coordinate coordinate = new Coordinate(2, 3);
        check(coordinate.row() == 2 && coordinate.column() == 3, "immutable state");
    }

    static class Animal {
        String category() { return "animal"; }
    }

    static final class Dog extends Animal {
        @Override String category() { return "dog"; }
    }

    static void example38Inheritance() {
        Dog dog = new Dog();
        check(dog instanceof Animal, "is a relationship");
    }

    static void example39MethodOverriding() {
        Dog dog = new Dog();
        check(dog.category().equals("dog"), "override");
    }

    static void example40RuntimePolymorphism() {
        Animal animal = new Dog();
        check(animal.category().equals("dog"), "dynamic dispatch");
    }

    abstract static class Parser {
        abstract int parse(String text);
        int parsePositive(String text) {
            int value = parse(text);
            if (value <= 0) throw new IllegalArgumentException();
            return value;
        }
    }

    static final class DecimalParser extends Parser {
        @Override int parse(String text) { return Integer.parseInt(text); }
    }

    static void example41AbstractClass() {
        check(new DecimalParser().parsePositive("7") == 7, "abstract class");
    }

    interface Formatter {
        String format(int value);
        default String label(int value) { return "value=" + format(value); }
    }

    static void example42Interface() {
        Formatter decimal = Integer::toString;
        check(decimal.label(9).equals("value=9"), "interface default method");
    }

    interface Clock {
        long nowMillis();
    }

    static final class ExpiringToken {
        private final Clock clock;
        private final long expiresAt;
        ExpiringToken(Clock clock, long expiresAt) { this.clock = clock; this.expiresAt = expiresAt; }
        boolean expired() { return clock.nowMillis() >= expiresAt; }
    }

    static void example43Composition() {
        Clock fixed = () -> 100L;
        check(new ExpiringToken(fixed, 90L).expired(), "composed clock");
    }

    static void example44WrapperParsing() {
        int value = Integer.parseInt("42");
        check(value == 42, "wrapper parsing");
    }

    static void example45AutoboxingAndUnboxing() {
        Integer boxed = 7;
        int primitive = boxed;
        check(primitive == 7, "boxing and unboxing");
    }

    static void example46IntegerCaching() {
        Integer first = 127;
        Integer second = 127;
        Integer highFirst = 128;
        Integer highSecond = 128;
        check(first == second, "required small cache");
        check(highFirst.equals(highSecond), "outside cache compare by value");
    }

    static final class Box<T> {
        private final T value;
        Box(T value) { this.value = value; }
        T value() { return value; }
    }

    static <T> T first(List<T> values) {
        if (values.isEmpty()) throw new IllegalArgumentException("empty");
        return values.get(0);
    }

    static void example47GenericClassAndMethod() {
        Box<String> box = new Box<>("java");
        check(box.value().equals("java") && first(List.of(4, 5)) == 4, "generics");
    }

    static void example48ArrayList() {
        List<String> values = new ArrayList<>();
        values.add("a");
        values.add("b");
        check(values.get(1).equals("b"), "array list");
    }

    static void example49HashSet() {
        Set<Integer> values = new HashSet<>(List.of(2, 2, 3));
        check(values.size() == 2, "set deduplication");
    }

    static void example50HashMapFrequency() {
        Map<String, Integer> frequency = new HashMap<>();
        for (String word : List.of("java", "dsa", "java")) {
            frequency.merge(word, 1, Integer::sum);
        }
        check(frequency.get("java") == 2, "frequency map");
    }

    static void example51QueueWithArrayDeque() {
        Deque<Integer> queue = new ArrayDeque<>();
        queue.offerLast(1);
        queue.offerLast(2);
        check(queue.pollFirst() == 1, "fifo queue");
    }

    static void example52StackWithArrayDeque() {
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(1);
        stack.push(2);
        check(stack.pop() == 2, "lifo stack");
    }

    static void example53PriorityQueue() {
        PriorityQueue<Integer> heap = new PriorityQueue<>(List.of(9, 3, 5));
        check(heap.poll() == 3, "min heap");
    }

    enum OrderStatus {
        CREATED, PAID, SHIPPED, DELIVERED, CANCELLED;
        boolean terminal() { return this == DELIVERED || this == CANCELLED; }
    }

    static void example54Enum() {
        check(OrderStatus.DELIVERED.terminal(), "enum behavior");
    }

    static int parsePositive(String text) throws IOException {
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IOException("invalid integer", exception);
        }
        if (value <= 0) throw new IOException("not positive");
        return value;
    }

    static void example55CheckedException() {
        try {
            check(parsePositive("4") == 4, "checked exception result");
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    static void requirePositive(int value) {
        if (value <= 0) throw new IllegalArgumentException("positive required");
    }

    static void example56UncheckedException() {
        boolean thrown = false;
        try {
            requirePositive(0);
        } catch (IllegalArgumentException exception) {
            thrown = true;
        }
        check(thrown, "unchecked precondition");
    }

    static final class FlagResource implements AutoCloseable {
        private final boolean[] closed;
        FlagResource(boolean[] closed) { this.closed = closed; }
        @Override public void close() { closed[0] = true; }
    }

    static void example57TryWithResources() {
        boolean[] closed = {false};
        try (FlagResource ignored = new FlagResource(closed)) {
            check(ignored != null && !closed[0], "open in body");
        }
        check(closed[0], "closed after body");
    }

    static void example58ScannerInput() {
        try (Scanner scanner = new Scanner("3 4 5 6")) {
            int length = scanner.nextInt();
            int sum = 0;
            for (int index = 0; index < length; index++) sum += scanner.nextInt();
            check(sum == 15, "scanner input");
        }
    }

    static void example59BufferedReaderInput() {
        try (BufferedReader reader = new BufferedReader(new StringReader("7 8\n"))) {
            String[] tokens = reader.readLine().split("\\s+");
            int sum = Integer.parseInt(tokens[0]) + Integer.parseInt(tokens[1]);
            check(sum == 15, "buffered reader input");
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    static long calculateSum(int[] numbers) {
        long sum = 0L;
        for (int number : numbers) sum += number;
        return sum;
    }

    static void example60InterviewQualityRefactoring() {
        int[] numbers = {Integer.MAX_VALUE, 1};
        check(calculateSum(numbers) == 2_147_483_648L, "clear name and safe accumulator");
    }

    static void example61ListOperations() {
        List<Integer> values = new ArrayList<>(List.of(10, 30));
        values.add(1, 20);
        values.set(0, 5);
        check(values.equals(List.of(5, 20, 30)), "list operations");
    }

    static void example62ListRemovalOverloads() {
        List<Integer> values = new ArrayList<>(List.of(10, 20, 30));
        values.remove(1);
        values.remove(Integer.valueOf(30));
        check(values.equals(List.of(10)), "index and value removal");
    }

    static void example63SetVariants() {
        Set<Integer> insertionOrdered = new LinkedHashSet<>(List.of(3, 1, 3, 2));
        Set<Integer> sorted = new TreeSet<>(insertionOrdered);
        check(insertionOrdered.toString().equals("[3, 1, 2]"), "linked set order");
        check(sorted.toString().equals("[1, 2, 3]"), "tree set order");
    }

    static void example64MapUpdateApis() {
        Map<String, List<Integer>> groups = new HashMap<>();
        groups.computeIfAbsent("odd", ignored -> new ArrayList<>()).add(3);
        Map<String, Integer> counts = new HashMap<>();
        counts.merge("java", 1, Integer::sum);
        counts.merge("java", 1, Integer::sum);
        check(groups.get("odd").equals(List.of(3)) && counts.get("java") == 2,
                "map update APIs");
    }

    static void example65MapIteration() {
        Map<String, Integer> ordered = new LinkedHashMap<>();
        ordered.put("a", 1);
        ordered.put("b", 2);
        int sum = 0;
        for (Map.Entry<String, Integer> entry : ordered.entrySet()) {
            check(!entry.getKey().isEmpty(), "entry key");
            sum += entry.getValue();
        }
        check(sum == 3, "entry iteration");
    }

    static void example66DequeConventions() {
        Deque<Integer> deque = new ArrayDeque<>();
        deque.offerLast(1);
        deque.offerLast(2);
        check(deque.pollFirst() == 1, "queue convention");
        deque.push(3);
        check(deque.pop() == 3, "stack convention");
    }

    static void example67PriorityQueueComparator() {
        PriorityQueue<Integer> maximums = new PriorityQueue<>(Comparator.reverseOrder());
        maximums.addAll(List.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE));
        check(maximums.poll() == Integer.MAX_VALUE, "safe max heap comparator");
    }

    static void example68CollectionFactories() {
        List<String> fixedSize = Arrays.asList("a", "b");
        fixedSize.set(0, "x");
        List<String> unmodifiable = List.of("a", "b");
        check(fixedSize.equals(List.of("x", "b")) && unmodifiable.size() == 2,
                "factory capabilities");
    }

    static void example69SafeIteratorRemoval() {
        List<Integer> values = new ArrayList<>(List.of(-1, 0, 2));
        for (Iterator<Integer> iterator = values.iterator(); iterator.hasNext();) {
            if (iterator.next() < 0) iterator.remove();
        }
        check(values.equals(List.of(0, 2)), "iterator removal");
    }

    static void example70CopiesAndViews() {
        List<String> source = new ArrayList<>(List.of("a"));
        List<String> snapshot = List.copyOf(source);
        List<String> view = Collections.unmodifiableList(source);
        source.add("b");
        check(snapshot.equals(List.of("a")), "copy is independent");
        check(view.equals(List.of("a", "b")), "view reflects source");
    }
}
