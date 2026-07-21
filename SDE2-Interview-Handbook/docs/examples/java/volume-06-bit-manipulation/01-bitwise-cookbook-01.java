public class BitOps {
    public static boolean isPowerOfTwo(int x) {
        return x > 0 && (x & (x - 1)) == 0;
    }

    public static int toggleBit(int x, int pos) {
        return x ^ (1 << pos);
    }
}
