import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/** Dependency-free executable checks for the Strings and String Patterns volume. */
public final class StringPatternsExamples {
    private static int checks;

    private StringPatternsExamples() {}

    public static void main(String[] args) {
        // String mechanics and conversion: checks 1-10.
        check(countLetter("banana", 'a') == 3);
        check(countLetter("", 'x') == 0);
        check(reverseCodePoints("Java").equals("avaJ"));
        check(reverseCodePoints("A\uD83D\uDE42").equals("\uD83D\uDE42A"));
        check("A\uD83D\uDE42".codePointCount(0, 3) == 2);
        check(joinWords(new String[] {"clear", "Java", "code"}).equals("clear Java code"));
        check(parseIntStrict("42") == 42);
        check(parseIntStrict(String.valueOf(Integer.MIN_VALUE)) == Integer.MIN_VALUE);
        check(parseIntStrict(String.valueOf(Integer.MAX_VALUE)) == Integer.MAX_VALUE);
        check(throwsNumberFormat(() -> parseIntStrict("2147483648")));

        // Two pointers and frequency: checks 11-25.
        check(isExactPalindrome("racecar"));
        check(isExactPalindrome(""));
        check(!isExactPalindrome("java"));
        check(isAsciiPhrasePalindrome("A man, a plan, a canal: Panama"));
        check(!isAsciiPhrasePalindrome("not a palindrome"));
        check(canBePalindromeAfterOneDeletion("abca"));
        check(!canBePalindromeAfterOneDeletion("abc"));
        check(areLowercaseAnagrams("listen", "silent"));
        check(!areLowercaseAnagrams("rat", "car"));
        check(longestCommonPrefix(new String[] {"flower", "flow", "flight"}).equals("fl"));
        check(longestCommonPrefix(new String[] {"dog", "racecar", "car"}).isEmpty());
        check(encodeRuns("aaabbc").equals("a3b2c1"));
        check(encodeRuns("").isEmpty());
        check(lowercaseSignature("eat").equals(lowercaseSignature("tea")));
        check(codePointFrequency("a\uD83D\uDE42a").get((int) 'a') == 2);

        // Sliding windows: checks 26-40.
        check(maximumVowels("abciiidef", 3) == 3);
        check(maximumVowels("abc", 0) == 0);
        check(longestUniqueAscii("abcabcbb") == 3);
        check(longestUniqueAscii("abba") == 2);
        check(longestUniqueSubstringAscii("pwwkew").equals("wke"));
        check(longestWithAtMostKDistinct("eceba", 2) == 3);
        check(longestWithAtMostKDistinct("abc", 0) == 0);
        check(countSubstringsWithExactlyKDistinct("pqpqs", 2) == 7);
        check(countSubstringsWithExactlyKDistinct("abc", 0) == 0);
        check(findLowercaseAnagrams("cbaebabacd", "abc").equals(List.of(0, 6)));
        check(findLowercaseAnagrams("ab", "abcd").isEmpty());
        check(minimumCoveringAsciiWindow("ADOBECODEBANC", "ABC").equals("BANC"));
        check(minimumCoveringAsciiWindow("a", "aa").isEmpty());
        check(minimumCoveringAsciiWindow("abc", "").isEmpty());
        check(longestRepeatedAfterReplacement("AABABBA", 1) == 4);

        // Pattern matching and regression: checks 41-50.
        check(naiveSearch("hello world", "world") == 6);
        check(naiveSearch("aaaa", "b") == -1);
        check(kmpSearch("hello world", "world") == 6);
        check(kmpSearch("abc", "") == 0);
        check(kmpAllMatches("aaaa", "aa").equals(List.of(0, 1, 2)));
        check(Arrays.equals(buildLps("ababaca"), new int[] {0, 0, 1, 2, 3, 0, 1}));
        check(rabinKarpSearchAscii("hello world", "world") == 6);
        check(rabinKarpSearchAscii("aaaa", "b") == -1);
        check(kmpMatchesNaiveOnRandomInputs());
        check("a,b,".split(",", -1).length == 3);

        if (checks != 50) {
            throw new AssertionError("expected 50 checks but observed " + checks);
        }
        System.out.println("PASS 50 Strings checks");
    }

    static int countLetter(String text, char target) {
        Objects.requireNonNull(text, "text");
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == target) {
                count++;
            }
        }
        return count;
    }

    static String reverseCodePoints(String text) {
        Objects.requireNonNull(text, "text");
        int[] codePoints = text.codePoints().toArray();
        StringBuilder result = new StringBuilder(text.length());
        for (int index = codePoints.length - 1; index >= 0; index--) {
            result.appendCodePoint(codePoints[index]);
        }
        return result.toString();
    }

    static String joinWords(String[] words) {
        Objects.requireNonNull(words, "words");
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < words.length; index++) {
            if (index > 0) {
                result.append(' ');
            }
            result.append(Objects.requireNonNull(words[index], "word"));
        }
        return result.toString();
    }

    static int parseIntStrict(String text) {
        if (text == null || text.isEmpty()) {
            throw new NumberFormatException("nonempty integer required");
        }
        int index = 0;
        boolean negative = false;
        char first = text.charAt(0);
        if (first == '-' || first == '+') {
            negative = first == '-';
            index++;
        }
        if (index == text.length()) {
            throw new NumberFormatException("digit required");
        }

        int limit = negative ? Integer.MIN_VALUE : -Integer.MAX_VALUE;
        int multiplicationLimit = limit / 10;
        int result = 0;
        while (index < text.length()) {
            char unit = text.charAt(index++);
            if (unit < '0' || unit > '9') {
                throw new NumberFormatException("invalid digit");
            }
            int digit = unit - '0';
            if (result < multiplicationLimit) {
                throw new NumberFormatException("overflow");
            }
            result *= 10;
            if (result < limit + digit) {
                throw new NumberFormatException("overflow");
            }
            result -= digit;
        }
        return negative ? result : -result;
    }

    static boolean throwsNumberFormat(Runnable action) {
        try {
            action.run();
            return false;
        } catch (NumberFormatException expected) {
            return true;
        }
    }

    static boolean isExactPalindrome(String text) {
        Objects.requireNonNull(text, "text");
        int left = 0;
        int right = text.length() - 1;
        while (left < right) {
            if (text.charAt(left++) != text.charAt(right--)) {
                return false;
            }
        }
        return true;
    }

    static boolean isAsciiPhrasePalindrome(String text) {
        if (text == null) {
            return false;
        }
        int left = 0;
        int right = text.length() - 1;
        while (left < right) {
            while (left < right && !isAsciiLetterOrDigit(text.charAt(left))) {
                left++;
            }
            while (left < right && !isAsciiLetterOrDigit(text.charAt(right))) {
                right--;
            }
            if (asciiLower(text.charAt(left)) != asciiLower(text.charAt(right))) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

    static boolean isAsciiLetterOrDigit(char unit) {
        return (unit >= 'A' && unit <= 'Z')
                || (unit >= 'a' && unit <= 'z')
                || (unit >= '0' && unit <= '9');
    }

    static char asciiLower(char unit) {
        return unit >= 'A' && unit <= 'Z' ? (char) (unit + ('a' - 'A')) : unit;
    }

    static boolean canBePalindromeAfterOneDeletion(String text) {
        Objects.requireNonNull(text, "text");
        int left = 0;
        int right = text.length() - 1;
        while (left < right && text.charAt(left) == text.charAt(right)) {
            left++;
            right--;
        }
        return left >= right
                || isPalindromeRange(text, left + 1, right)
                || isPalindromeRange(text, left, right - 1);
    }

    static boolean isPalindromeRange(String text, int left, int rightInclusive) {
        while (left < rightInclusive) {
            if (text.charAt(left++) != text.charAt(rightInclusive--)) {
                return false;
            }
        }
        return true;
    }

    static boolean areLowercaseAnagrams(String first, String second) {
        if (first == null || second == null || first.length() != second.length()) {
            return false;
        }
        int[] difference = new int[26];
        for (int index = 0; index < first.length(); index++) {
            difference[requireLower(first.charAt(index))]++;
            difference[requireLower(second.charAt(index))]--;
        }
        for (int value : difference) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    static Map<Integer, Integer> codePointFrequency(String text) {
        Map<Integer, Integer> frequency = new HashMap<>();
        text.codePoints().forEach(codePoint -> frequency.merge(codePoint, 1, Integer::sum));
        return frequency;
    }

    static String lowercaseSignature(String word) {
        int[] frequency = new int[26];
        for (int index = 0; index < word.length(); index++) {
            frequency[requireLower(word.charAt(index))]++;
        }
        StringBuilder signature = new StringBuilder();
        for (int count : frequency) {
            signature.append('#').append(count);
        }
        return signature.toString();
    }

    static String longestCommonPrefix(String[] words) {
        if (words == null || words.length == 0) {
            return "";
        }
        Objects.requireNonNull(words[0], "word");
        int prefixLength = words[0].length();
        for (int wordIndex = 1; wordIndex < words.length; wordIndex++) {
            String word = Objects.requireNonNull(words[wordIndex], "word");
            prefixLength = Math.min(prefixLength, word.length());
            int index = 0;
            while (index < prefixLength && words[0].charAt(index) == word.charAt(index)) {
                index++;
            }
            prefixLength = index;
            if (prefixLength == 0) {
                return "";
            }
        }
        return words[0].substring(0, prefixLength);
    }

    static String encodeRuns(String text) {
        Objects.requireNonNull(text, "text");
        if (text.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        int runStart = 0;
        for (int index = 1; index <= text.length(); index++) {
            if (index == text.length() || text.charAt(index) != text.charAt(runStart)) {
                result.append(text.charAt(runStart)).append(index - runStart);
                runStart = index;
            }
        }
        return result.toString();
    }

    static int maximumVowels(String text, int k) {
        if (k < 0 || k > text.length()) {
            throw new IllegalArgumentException("k must be in [0, length]");
        }
        int current = 0;
        for (int index = 0; index < k; index++) {
            current += isAsciiVowel(text.charAt(index)) ? 1 : 0;
        }
        int best = current;
        for (int right = k; right < text.length(); right++) {
            current += isAsciiVowel(text.charAt(right)) ? 1 : 0;
            current -= isAsciiVowel(text.charAt(right - k)) ? 1 : 0;
            best = Math.max(best, current);
        }
        return best;
    }

    static boolean isAsciiVowel(char unit) {
        return unit == 'a' || unit == 'e' || unit == 'i' || unit == 'o' || unit == 'u';
    }

    static int longestUniqueAscii(String text) {
        int[] frequency = new int[128];
        int left = 0;
        int best = 0;
        for (int right = 0; right < text.length(); right++) {
            char entered = requireAscii(text.charAt(right));
            frequency[entered]++;
            while (frequency[entered] > 1) {
                frequency[text.charAt(left++)]--;
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    static String longestUniqueSubstringAscii(String text) {
        int[] nextAfterLast = new int[128];
        Arrays.fill(nextAfterLast, -1);
        int left = 0;
        int bestStart = 0;
        int bestLength = 0;
        for (int right = 0; right < text.length(); right++) {
            char unit = requireAscii(text.charAt(right));
            left = Math.max(left, nextAfterLast[unit]);
            int length = right - left + 1;
            if (length > bestLength) {
                bestStart = left;
                bestLength = length;
            }
            nextAfterLast[unit] = right + 1;
        }
        return text.substring(bestStart, bestStart + bestLength);
    }

    static int longestWithAtMostKDistinct(String text, int k) {
        if (k < 0) {
            throw new IllegalArgumentException("k must be nonnegative");
        }
        Map<Character, Integer> frequency = new HashMap<>();
        int left = 0;
        int best = 0;
        for (int right = 0; right < text.length(); right++) {
            frequency.merge(text.charAt(right), 1, Integer::sum);
            while (frequency.size() > k) {
                char removed = text.charAt(left++);
                int next = frequency.get(removed) - 1;
                if (next == 0) {
                    frequency.remove(removed);
                } else {
                    frequency.put(removed, next);
                }
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    static long countSubstringsWithExactlyKDistinct(String text, int k) {
        if (k <= 0) {
            return 0;
        }
        return countAtMostDistinct(text, k) - countAtMostDistinct(text, k - 1);
    }

    static long countAtMostDistinct(String text, int k) {
        Map<Character, Integer> frequency = new HashMap<>();
        int left = 0;
        long count = 0;
        for (int right = 0; right < text.length(); right++) {
            frequency.merge(text.charAt(right), 1, Integer::sum);
            while (frequency.size() > k) {
                char removed = text.charAt(left++);
                int old = frequency.get(removed);
                if (old == 1) {
                    frequency.remove(removed);
                } else {
                    frequency.put(removed, old - 1);
                }
            }
            count += right - left + 1L;
        }
        return count;
    }

    static List<Integer> findLowercaseAnagrams(String text, String pattern) {
        List<Integer> starts = new ArrayList<>();
        if (pattern.isEmpty() || pattern.length() > text.length()) {
            return starts;
        }
        int[] difference = new int[26];
        int nonZero = 0;
        for (int index = 0; index < pattern.length(); index++) {
            nonZero = adjust(difference, requireLower(pattern.charAt(index)), 1, nonZero);
            nonZero = adjust(difference, requireLower(text.charAt(index)), -1, nonZero);
        }
        if (nonZero == 0) {
            starts.add(0);
        }
        for (int right = pattern.length(); right < text.length(); right++) {
            nonZero = adjust(difference,
                    requireLower(text.charAt(right - pattern.length())), 1, nonZero);
            nonZero = adjust(difference, requireLower(text.charAt(right)), -1, nonZero);
            if (nonZero == 0) {
                starts.add(right - pattern.length() + 1);
            }
        }
        return starts;
    }

    static int adjust(int[] values, int index, int delta, int nonZero) {
        if (values[index] != 0) {
            nonZero--;
        }
        values[index] += delta;
        if (values[index] != 0) {
            nonZero++;
        }
        return nonZero;
    }

    static int requireLower(char unit) {
        if (unit < 'a' || unit > 'z') {
            throw new IllegalArgumentException("lowercase English letters required");
        }
        return unit - 'a';
    }

    static String minimumCoveringAsciiWindow(String text, String target) {
        if (target.isEmpty()) {
            return "";
        }
        int[] need = new int[128];
        int requiredTypes = 0;
        for (int index = 0; index < target.length(); index++) {
            char unit = requireAscii(target.charAt(index));
            if (need[unit]++ == 0) {
                requiredTypes++;
            }
        }
        int[] have = new int[128];
        int formedTypes = 0;
        int left = 0;
        int bestStart = 0;
        int bestLength = Integer.MAX_VALUE;
        for (int right = 0; right < text.length(); right++) {
            char entered = requireAscii(text.charAt(right));
            have[entered]++;
            if (need[entered] > 0 && have[entered] == need[entered]) {
                formedTypes++;
            }
            while (formedTypes == requiredTypes) {
                int length = right - left + 1;
                if (length < bestLength) {
                    bestStart = left;
                    bestLength = length;
                }
                char removed = text.charAt(left++);
                if (need[removed] > 0 && have[removed] == need[removed]) {
                    formedTypes--;
                }
                have[removed]--;
            }
        }
        return bestLength == Integer.MAX_VALUE
                ? ""
                : text.substring(bestStart, bestStart + bestLength);
    }

    static int longestRepeatedAfterReplacement(String text, int k) {
        if (k < 0) {
            throw new IllegalArgumentException("k must be nonnegative");
        }
        int[] frequency = new int[26];
        int left = 0;
        int maximumFrequencySeen = 0;
        int best = 0;
        for (int right = 0; right < text.length(); right++) {
            char unit = text.charAt(right);
            if (unit < 'A' || unit > 'Z') {
                throw new IllegalArgumentException("uppercase English letters required");
            }
            maximumFrequencySeen = Math.max(
                    maximumFrequencySeen, ++frequency[unit - 'A']);
            while (right - left + 1 - maximumFrequencySeen > k) {
                frequency[text.charAt(left++) - 'A']--;
            }
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    static char requireAscii(char unit) {
        if (unit >= 128) {
            throw new IllegalArgumentException("ASCII required");
        }
        return unit;
    }

    static int naiveSearch(String text, String pattern) {
        if (pattern.isEmpty()) {
            return 0;
        }
        if (pattern.length() > text.length()) {
            return -1;
        }
        int lastStart = text.length() - pattern.length();
        for (int start = 0; start <= lastStart; start++) {
            int matched = 0;
            while (matched < pattern.length()
                    && text.charAt(start + matched) == pattern.charAt(matched)) {
                matched++;
            }
            if (matched == pattern.length()) {
                return start;
            }
        }
        return -1;
    }

    static int[] buildLps(String pattern) {
        int[] lps = new int[pattern.length()];
        int prefixLength = 0;
        for (int index = 1; index < pattern.length();) {
            if (pattern.charAt(index) == pattern.charAt(prefixLength)) {
                lps[index++] = ++prefixLength;
            } else if (prefixLength > 0) {
                prefixLength = lps[prefixLength - 1];
            } else {
                lps[index++] = 0;
            }
        }
        return lps;
    }

    static int kmpSearch(String text, String pattern) {
        if (pattern.isEmpty()) {
            return 0;
        }
        int[] lps = buildLps(pattern);
        int textIndex = 0;
        int matched = 0;
        while (textIndex < text.length()) {
            if (text.charAt(textIndex) == pattern.charAt(matched)) {
                textIndex++;
                matched++;
                if (matched == pattern.length()) {
                    return textIndex - matched;
                }
            } else if (matched > 0) {
                matched = lps[matched - 1];
            } else {
                textIndex++;
            }
        }
        return -1;
    }

    static List<Integer> kmpAllMatches(String text, String pattern) {
        List<Integer> matches = new ArrayList<>();
        if (pattern.isEmpty()) {
            for (int index = 0; index <= text.length(); index++) {
                matches.add(index);
            }
            return matches;
        }
        int[] lps = buildLps(pattern);
        int textIndex = 0;
        int matched = 0;
        while (textIndex < text.length()) {
            if (text.charAt(textIndex) == pattern.charAt(matched)) {
                textIndex++;
                matched++;
                if (matched == pattern.length()) {
                    matches.add(textIndex - matched);
                    matched = lps[matched - 1];
                }
            } else if (matched > 0) {
                matched = lps[matched - 1];
            } else {
                textIndex++;
            }
        }
        return matches;
    }

    static int rabinKarpSearchAscii(String text, String pattern) {
        if (pattern.isEmpty()) {
            return 0;
        }
        if (pattern.length() > text.length()) {
            return -1;
        }
        final long base = 257;
        final long modulus = 1_000_000_007L;
        long highestPower = 1;
        long patternHash = 0;
        long windowHash = 0;
        for (int index = 0; index < pattern.length(); index++) {
            requireAscii(pattern.charAt(index));
            requireAscii(text.charAt(index));
            patternHash = (patternHash * base + pattern.charAt(index)) % modulus;
            windowHash = (windowHash * base + text.charAt(index)) % modulus;
            if (index + 1 < pattern.length()) {
                highestPower = highestPower * base % modulus;
            }
        }
        for (int start = 0; start <= text.length() - pattern.length(); start++) {
            if (windowHash == patternHash
                    && text.regionMatches(start, pattern, 0, pattern.length())) {
                return start;
            }
            if (start < text.length() - pattern.length()) {
                long outgoing = text.charAt(start) * highestPower % modulus;
                windowHash = (windowHash - outgoing + modulus) % modulus;
                char incoming = requireAscii(text.charAt(start + pattern.length()));
                windowHash = (windowHash * base + incoming) % modulus;
            }
        }
        return -1;
    }

    static boolean kmpMatchesNaiveOnRandomInputs() {
        Random random = new Random(7_202_026L);
        for (int test = 0; test < 2_000; test++) {
            String text = randomLowercase(random, random.nextInt(20));
            String pattern = randomLowercase(random, random.nextInt(8));
            if (naiveSearch(text, pattern) != kmpSearch(text, pattern)) {
                return false;
            }
        }
        return true;
    }

    static String randomLowercase(Random random, int length) {
        StringBuilder result = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            result.append((char) ('a' + random.nextInt(4)));
        }
        return result.toString();
    }

    static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("check " + (checks + 1) + " failed");
        }
        checks++;
    }
}
