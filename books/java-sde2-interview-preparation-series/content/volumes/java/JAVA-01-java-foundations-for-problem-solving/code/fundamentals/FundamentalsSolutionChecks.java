import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Executable checks for the Wave 1 Java Fundamentals solution studio. */
public final class FundamentalsSolutionChecks {
    private FundamentalsSolutionChecks() {}

    public static void main(String[] args) {
        check(safeAverage(new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE})
                == Integer.MAX_VALUE, "safe average");
        check(safeMultiply(100_000, 100_000) == 10_000_000_000L,
                "safe multiply");

        Person original = new Person("Original");
        rename(original);
        replace(original);
        check(original.name.equals("Updated"), "reference reassignment");

        int[][] source = {{1, 2}, null, {3}};
        int[][] copy = deepCopy(source);
        copy[0][0] = 99;
        check(source[0][0] == 1 && copy[1] == null, "jagged deep copy");

        check(decimalDigit('7') == 7, "digit conversion");
        expectFailure(() -> decimalDigit('x'), "invalid decimal digit");

        BankAccount account = new BankAccount(500);
        account.withdraw(125);
        check(account.balanceCents() == 375, "account invariant");
        expectFailure(() -> account.withdraw(500), "invalid withdrawal");

        check(last(List.of("a", "b")).equals("b"), "generic last");
        check(mostFrequent(List.of("pear", "apple", "pear", "apple"))
                .equals("apple"), "deterministic frequency tie");

        check(Arrays.equals(parseRow("10 20 30"), new int[] {10, 20, 30}),
                "parse row");
        expectFailure(() -> parseRow("10 nope"), "contextual parse failure");

        List<Candidate> candidates = new ArrayList<>(List.of(
                new Candidate("Grace", Integer.MIN_VALUE),
                new Candidate("Ada", Integer.MAX_VALUE),
                new Candidate("Lin", Integer.MAX_VALUE)));
        candidates.sort(CANDIDATE_ORDER);
        check(candidates.get(0).name().equals("Ada")
                        && candidates.get(1).name().equals("Lin"),
                "safe deterministic comparator");

        check(Arrays.equals(
                        findTwoSumIndices(new int[] {Integer.MIN_VALUE, 0, 7}, 7),
                        new int[] {1, 2}),
                "overflow-safe complement");

        System.out.println("PASS 11 Java Fundamentals solution checks");
    }

    static double safeAverage(int[] numbers) {
        Objects.requireNonNull(numbers, "numbers");
        if (numbers.length == 0) {
            throw new IllegalArgumentException("numbers must not be empty");
        }
        long sum = 0L;
        for (int number : numbers) {
            sum += number;
        }
        return (double) sum / numbers.length;
    }

    static long safeMultiply(int left, int right) {
        return (long) left * right;
    }

    static final class Person {
        private String name;

        Person(String name) {
            this.name = Objects.requireNonNull(name, "name");
        }
    }

    static void rename(Person person) {
        person.name = "Updated";
    }

    static void replace(Person person) {
        person = new Person("Replacement");
    }

    static int[][] deepCopy(int[][] matrix) {
        Objects.requireNonNull(matrix, "matrix");
        int[][] copy = new int[matrix.length][];
        for (int row = 0; row < matrix.length; row++) {
            copy[row] = matrix[row] == null ? null : matrix[row].clone();
        }
        return copy;
    }

    static int decimalDigit(char character) {
        int value = Character.digit(character, 10);
        if (value < 0) {
            throw new IllegalArgumentException("not a decimal digit: " + character);
        }
        return value;
    }

    static final class BankAccount {
        private long balanceCents;

        BankAccount(long openingBalanceCents) {
            if (openingBalanceCents < 0) {
                throw new IllegalArgumentException("negative opening balance");
            }
            balanceCents = openingBalanceCents;
        }

        void withdraw(long cents) {
            if (cents <= 0 || cents > balanceCents) {
                throw new IllegalArgumentException("invalid withdrawal");
            }
            balanceCents -= cents;
        }

        long balanceCents() {
            return balanceCents;
        }
    }

    static <T> T last(List<T> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }
        return values.get(values.size() - 1);
    }

    static String mostFrequent(List<String> words) {
        Objects.requireNonNull(words, "words");
        if (words.isEmpty()) {
            throw new IllegalArgumentException("words must not be empty");
        }

        Map<String, Integer> frequencies = new HashMap<>();
        for (String word : words) {
            frequencies.merge(Objects.requireNonNull(word, "word"), 1, Integer::sum);
        }

        String bestWord = null;
        int bestCount = -1;
        for (Map.Entry<String, Integer> entry : frequencies.entrySet()) {
            String word = entry.getKey();
            int count = entry.getValue();
            if (count > bestCount
                    || (count == bestCount && word.compareTo(bestWord) < 0)) {
                bestWord = word;
                bestCount = count;
            }
        }
        return bestWord;
    }

    static int[] parseRow(String line) {
        Objects.requireNonNull(line, "line");
        if (line.isBlank()) {
            return new int[0];
        }
        String[] tokens = line.trim().split("\\s+");
        int[] values = new int[tokens.length];
        for (int index = 0; index < tokens.length; index++) {
            try {
                values[index] = Integer.parseInt(tokens[index]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "invalid integer at token " + index + ": " + tokens[index],
                        exception);
            }
        }
        return values;
    }

    record Candidate(String name, int score) {}

    private static final Comparator<Candidate> CANDIDATE_ORDER =
            Comparator.comparingInt(Candidate::score)
                    .reversed()
                    .thenComparing(Candidate::name);

    static int[] findTwoSumIndices(int[] numbers, int target) {
        Objects.requireNonNull(numbers, "numbers");
        Map<Integer, Integer> earliestIndex = new HashMap<>();
        for (int index = 0; index < numbers.length; index++) {
            long complement = (long) target - numbers[index];
            if (complement >= Integer.MIN_VALUE && complement <= Integer.MAX_VALUE) {
                Integer earlier = earliestIndex.get((int) complement);
                if (earlier != null) {
                    return new int[] {earlier, index};
                }
            }
            earliestIndex.putIfAbsent(numbers[index], index);
        }
        return new int[0];
    }

    private static void expectFailure(Runnable operation, String message) {
        try {
            operation.run();
            throw new AssertionError("expected failure: " + message);
        } catch (IllegalArgumentException expected) {
            // Expected by this check.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
