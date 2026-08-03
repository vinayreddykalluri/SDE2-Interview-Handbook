import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class BitManipulationExamples {
    private static int checks;

    private BitManipulationExamples() {
    }

    static String bits32(int value) {
        StringBuilder result = new StringBuilder(Integer.SIZE);
        for (int bit = Integer.SIZE - 1; bit >= 0; bit--) {
            result.append((value >>> bit) & 1);
        }
        return result.toString();
    }

    static long oneBit(int index) {
        if (index < 0 || index >= Long.SIZE) {
            throw new IllegalArgumentException("index must be in [0, 63]");
        }
        return 1L << index;
    }

    static boolean isSet(long value, int index) {
        return (value & oneBit(index)) != 0;
    }

    static long set(long value, int index) {
        return value | oneBit(index);
    }

    static long clear(long value, int index) {
        return value & ~oneBit(index);
    }

    static long toggle(long value, int index) {
        return value ^ oneBit(index);
    }

    static long lowBitsMask(int width) {
        if (width < 0 || width > Long.SIZE) {
            throw new IllegalArgumentException("width must be in [0, 64]");
        }
        if (width == 0) {
            return 0L;
        }
        return width == Long.SIZE ? -1L : (1L << width) - 1;
    }

    static long extractField(long word, int offset, int width) {
        validateField(offset, width);
        return width == Long.SIZE
                ? word
                : (word >>> offset) & lowBitsMask(width);
    }

    static long replaceField(long word, int offset, int width, long value) {
        validateField(offset, width);
        long lowMask = lowBitsMask(width);
        if ((value & ~lowMask) != 0) {
            throw new IllegalArgumentException("value does not fit field");
        }
        if (width == Long.SIZE) {
            return value;
        }
        long mask = lowMask << offset;
        return (word & ~mask) | (value << offset);
    }

    private static void validateField(int offset, int width) {
        if (offset < 0 || width < 1 || width > Long.SIZE
                || offset > Long.SIZE - width) {
            throw new IllegalArgumentException("invalid field bounds");
        }
    }

    static int countSetBits(int value) {
        int count = 0;
        while (value != 0) {
            value &= value - 1;
            count++;
        }
        return count;
    }

    static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    static boolean isPowerOfFour(int value) {
        return isPowerOfTwo(value) && (value & 0x5555_5555) != 0;
    }

    static int hammingDistance(int first, int second) {
        return Integer.bitCount(first ^ second);
    }

    static int reverseBits(int value) {
        int reversed = 0;
        for (int bit = 0; bit < Integer.SIZE; bit++) {
            reversed = (reversed << 1) | (value & 1);
            value >>>= 1;
        }
        return reversed;
    }

    static int[] countBitsThrough(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be nonnegative");
        }
        int[] bits = new int[n + 1];
        for (int value = 1; value <= n; value++) {
            bits[value] = bits[value & (value - 1)] + 1;
        }
        return bits;
    }

    static int singleAmongPairs(int[] values) {
        requireNonEmpty(values);
        int answer = 0;
        for (int value : values) {
            answer ^= value;
        }
        return answer;
    }

    static int missingFromZeroThroughN(int[] values) {
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
        int answer = values.length;
        for (int index = 0; index < values.length; index++) {
            answer ^= index;
            answer ^= values[index];
        }
        return answer;
    }

    static int[] twoSinglesAmongPairs(int[] values) {
        requireNonEmpty(values);
        int combined = 0;
        for (int value : values) {
            combined ^= value;
        }
        if (combined == 0) {
            throw new IllegalArgumentException("distinct singles required");
        }
        int distinguishing = combined & -combined;
        int first = 0;
        int second = 0;
        for (int value : values) {
            if ((value & distinguishing) == 0) {
                first ^= value;
            } else {
                second ^= value;
            }
        }
        return first <= second
                ? new int[] {first, second}
                : new int[] {second, first};
    }

    static int singleAmongTriples(int[] values) {
        requireNonEmpty(values);
        int answer = 0;
        for (int bit = 0; bit < Integer.SIZE; bit++) {
            int count = 0;
            for (int value : values) {
                count += (value >>> bit) & 1;
            }
            if (count % 3 != 0) {
                answer |= 1 << bit;
            }
        }
        return answer;
    }

    static int[] buildPrefixXor(int[] values) {
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
        int[] prefix = new int[values.length + 1];
        for (int index = 0; index < values.length; index++) {
            prefix[index + 1] = prefix[index] ^ values[index];
        }
        return prefix;
    }

    static int rangeXor(int[] prefix, int left, int right) {
        if (prefix == null || left < 0 || right < left
                || right + 1 >= prefix.length) {
            throw new IllegalArgumentException("invalid prefix or range");
        }
        return prefix[right + 1] ^ prefix[left];
    }

    static int xorZeroThrough(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be nonnegative");
        }
        return switch (n & 3) {
            case 0 -> n;
            case 1 -> 1;
            case 2 -> n + 1;
            default -> 0;
        };
    }

    static int xorRange(int left, int right) {
        if (left < 0 || right < left) {
            throw new IllegalArgumentException("invalid nonnegative range");
        }
        return xorZeroThrough(right)
                ^ (left == 0 ? 0 : xorZeroThrough(left - 1));
    }

    static long countSubarraysWithXor(int[] values, int target) {
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
        Map<Integer, Integer> frequency = new HashMap<>();
        frequency.put(0, 1);
        int prefix = 0;
        long count = 0;
        for (int value : values) {
            prefix ^= value;
            count += frequency.getOrDefault(prefix ^ target, 0);
            frequency.merge(prefix, 1, Integer::sum);
        }
        return count;
    }

    static int countSubmasksIncludingZero(int mask) {
        int count = 0;
        int sub = mask;
        while (true) {
            count++;
            if (sub == 0) {
                return count;
            }
            sub = (sub - 1) & mask;
        }
    }

    static int grayCode(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be nonnegative");
        }
        return index ^ (index >>> 1);
    }

    private static final class BitNode {
        private final BitNode[] next = new BitNode[2];
    }

    static int maximumXorPair(int[] values) {
        if (values == null || values.length < 2) {
            throw new IllegalArgumentException("at least two values required");
        }
        BitNode root = new BitNode();
        for (int value : values) {
            if (value < 0) {
                throw new IllegalArgumentException("nonnegative values required");
            }
            BitNode node = root;
            for (int bit = 30; bit >= 0; bit--) {
                int current = (value >>> bit) & 1;
                if (node.next[current] == null) {
                    node.next[current] = new BitNode();
                }
                node = node.next[current];
            }
        }
        int best = 0;
        for (int value : values) {
            BitNode node = root;
            int candidate = 0;
            for (int bit = 30; bit >= 0; bit--) {
                int current = (value >>> bit) & 1;
                int preferred = current ^ 1;
                if (node.next[preferred] != null) {
                    candidate |= 1 << bit;
                    node = node.next[preferred];
                } else {
                    node = node.next[current];
                }
            }
            best = Math.max(best, candidate);
        }
        return best;
    }

    static int rangeBitwiseAnd(int left, int right) {
        if (left < 0 || right < left) {
            throw new IllegalArgumentException("invalid nonnegative range");
        }
        int shifts = 0;
        while (left != right) {
            left >>>= 1;
            right >>>= 1;
            shifts++;
        }
        return left << shifts;
    }

    static long totalSetBitsThrough(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be nonnegative");
        }
        if (n == 0) {
            return 0;
        }
        int highestBit = 31 - Integer.numberOfLeadingZeros(n);
        int power = 1 << highestBit;
        long fullBlock = highestBit == 0
                ? 0
                : (long) highestBit * (power >>> 1);
        return fullBlock + (long) n - power + 1
                + totalSetBitsThrough(n - power);
    }

    static int significantComplement(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("nonnegative value required");
        }
        if (value == 0) {
            return 1;
        }
        int highest = Integer.highestOneBit(value);
        int mask = (highest << 1) - 1;
        return value ^ mask;
    }

    static int minimumXorPair(int[] input) {
        if (input == null || input.length < 2) {
            throw new IllegalArgumentException("at least two values required");
        }
        int[] values = input.clone();
        for (int value : values) {
            if (value < 0) {
                throw new IllegalArgumentException("nonnegative values required");
            }
        }
        Arrays.sort(values);
        int best = Integer.MAX_VALUE;
        for (int index = 1; index < values.length; index++) {
            best = Math.min(best, values[index - 1] ^ values[index]);
        }
        return best;
    }

    static int distinctSubarrayOrCount(int[] values) {
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
        Set<Integer> all = new HashSet<>();
        Set<Integer> previous = Set.of();
        for (int value : values) {
            Set<Integer> current = new HashSet<>();
            current.add(value);
            for (int prior : previous) {
                current.add(prior | value);
            }
            all.addAll(current);
            previous = current;
        }
        return all.size();
    }

    static int addWithBits(int first, int second) {
        while (second != 0) {
            int carry = (first & second) << 1;
            first ^= second;
            second = carry;
        }
        return first;
    }

    private static void requireNonEmpty(int[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("check " + (checks + 1) + " failed");
        }
        checks++;
    }

    private static void checkThrows(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }
        throw new AssertionError("check " + (checks + 1) + " failed");
    }

    public static void main(String[] args) {
        check(bits32(5).equals("00000000000000000000000000000101"));
        check(bits32(-1).equals("11111111111111111111111111111111"));
        check(isSet(1L << 63, 63));
        check(set(0, 40) == 1L << 40);
        check(clear(0b1111, 2) == 0b1011);
        check(toggle(toggle(123, 6), 6) == 123);
        checkThrows(() -> oneBit(64));
        check(lowBitsMask(0) == 0);
        check(lowBitsMask(5) == 0b1_1111);
        check(lowBitsMask(64) == -1L);
        check(extractField(0b1101_0110, 2, 3) == 5);
        check(replaceField(0b1111_0000, 4, 4, 5) == 0b0101_0000);
        checkThrows(() -> replaceField(0, 3, 2, 4));
        check(countSetBits(0) == 0);
        check(countSetBits(-1) == 32);
        check(isPowerOfTwo(1 << 30));
        check(!isPowerOfTwo(0));
        check(!isPowerOfTwo(Integer.MIN_VALUE));
        check(isPowerOfFour(64));
        check(!isPowerOfFour(8));
        check(hammingDistance(0b1010, 0b0011) == 2);
        check(reverseBits(1) == Integer.MIN_VALUE);
        check(Arrays.equals(countBitsThrough(5), new int[] {0, 1, 1, 2, 1, 2}));
        check(singleAmongPairs(new int[] {7, -2, 7}) == -2);
        check(missingFromZeroThroughN(new int[] {3, 0, 1}) == 2);
        check(Arrays.equals(
                twoSinglesAmongPairs(new int[] {1, 2, 1, 3, 2, 5}),
                new int[] {3, 5}));
        check(singleAmongTriples(new int[] {6, -9, 6, 6}) == -9);
        int[] prefix = buildPrefixXor(new int[] {4, 2, 7, 2});
        check(rangeXor(prefix, 1, 3) == 7);
        check(xorZeroThrough(6) == 7);
        check(xorRange(3, 6) == (3 ^ 4 ^ 5 ^ 6));
        check(countSubarraysWithXor(new int[] {4, 2, 2, 6, 4}, 6) == 4);
        check(countSubmasksIncludingZero(0b10110) == 8);
        check(Integer.bitCount(grayCode(6) ^ grayCode(7)) == 1);
        check(maximumXorPair(new int[] {3, 10, 5, 25, 2, 8}) == 28);
        check(rangeBitwiseAnd(26, 30) == 24);
        check(totalSetBitsThrough(13) == 25);
        check(significantComplement(5) == 2);
        check(minimumXorPair(new int[] {9, 5, 3}) == 6);
        check(distinctSubarrayOrCount(new int[] {1, 2}) == 3);
        AtomicLong flags = new AtomicLong();
        flags.getAndUpdate(current -> current | (1L << 7));
        BitSet bitSet = BitSet.valueOf(new long[] {flags.get()});
        check(bitSet.get(7) && addWithBits(-4, 9) == 5);

        if (checks != 40) {
            throw new AssertionError("expected 40 checks, found " + checks);
        }
        System.out.println("PASS 40 Bit Manipulation checks");
    }
}
