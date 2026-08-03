import java.util.Arrays;

public final class StringContractChecks {
    private StringContractChecks() {
    }

    static int parseIntManual(String text) {
        if (text == null || text.isEmpty()) {
            throw new NumberFormatException("empty input");
        }
        int index = 0;
        boolean negative = false;
        char first = text.charAt(0);
        if (first == '-' || first == '+') {
            negative = first == '-';
            index++;
        }
        if (index == text.length()) {
            throw new NumberFormatException("sign without digits");
        }

        int limit = negative ? Integer.MIN_VALUE : -Integer.MAX_VALUE;
        int result = 0;
        int multiplicationLimit = limit / 10;
        while (index < text.length()) {
            char character = text.charAt(index++);
            if (character < '0' || character > '9') {
                throw new NumberFormatException("invalid digit: " + character);
            }
            int digit = character - '0';
            if (result < multiplicationLimit) {
                throw new NumberFormatException("integer overflow");
            }
            result *= 10;
            if (result < limit + digit) {
                throw new NumberFormatException("integer overflow");
            }
            result -= digit;
        }
        return negative ? result : -result;
    }

    static int indexOfKmp(String text, String pattern) {
        if (text == null || pattern == null) {
            throw new IllegalArgumentException("text and pattern must be non-null");
        }
        if (pattern.isEmpty()) {
            return 0;
        }
        int[] prefix = prefixTable(pattern);
        int matched = 0;
        for (int index = 0; index < text.length(); index++) {
            while (matched > 0 && text.charAt(index) != pattern.charAt(matched)) {
                matched = prefix[matched - 1];
            }
            if (text.charAt(index) == pattern.charAt(matched)) {
                matched++;
                if (matched == pattern.length()) {
                    return index - pattern.length() + 1;
                }
            }
        }
        return -1;
    }

    private static int[] prefixTable(String pattern) {
        int[] prefix = new int[pattern.length()];
        int matched = 0;
        for (int index = 1; index < pattern.length(); index++) {
            while (matched > 0 && pattern.charAt(index) != pattern.charAt(matched)) {
                matched = prefix[matched - 1];
            }
            if (pattern.charAt(index) == pattern.charAt(matched)) {
                prefix[index] = ++matched;
            }
        }
        return prefix;
    }

    static String reverseCodePoints(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must be non-null");
        }
        int[] codePoints = text.codePoints().toArray();
        StringBuilder result = new StringBuilder(text.length());
        for (int index = codePoints.length - 1; index >= 0; index--) {
            result.appendCodePoint(codePoints[index]);
        }
        return result.toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireSameParse(String input) {
        require(parseIntManual(input) == Integer.parseInt(input),
                "parser mismatch for " + input);
    }

    private static void requireBothReject(String input) {
        boolean manualRejected = false;
        boolean jdkRejected = false;
        try {
            parseIntManual(input);
        } catch (NumberFormatException expected) {
            manualRejected = true;
        }
        try {
            Integer.parseInt(input);
        } catch (NumberFormatException expected) {
            jdkRejected = true;
        }
        require(manualRejected && jdkRejected, "both parsers must reject " + input);
    }

    public static void main(String[] args) {
        for (String input : Arrays.asList(
                "0", "+0", "-0", "7", "-42",
                "2147483647", "-2147483648")) {
            requireSameParse(input);
        }
        for (String input : Arrays.asList(
                "", "+", "-", " 1", "1 ", "12x", "2147483648", "-2147483649")) {
            requireBothReject(input);
        }

        String[] texts = {"", "a", "aaaa", "ababaca", "find a needle here"};
        String[] patterns = {"", "a", "aa", "aca", "needle", "missing"};
        for (String text : texts) {
            for (String pattern : patterns) {
                require(indexOfKmp(text, pattern) == text.indexOf(pattern),
                        "KMP mismatch: text=" + text + ", pattern=" + pattern);
            }
        }

        String supplementary = "A\uD83D\uDE03B";
        require(reverseCodePoints(supplementary).equals("B\uD83D\uDE03A"),
                "code-point reversal split a surrogate pair");

        System.out.println("PASS string parsing, KMP, and code-point checks");
    }
}
