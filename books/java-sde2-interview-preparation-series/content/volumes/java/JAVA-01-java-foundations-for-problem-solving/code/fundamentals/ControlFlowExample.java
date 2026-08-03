public final class ControlFlowExample {
    static int firstAtLeast(int[] values, int threshold) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] >= threshold) {
                return index;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        int[] values = {3, 5, 8, 13};
        System.out.println(firstAtLeast(values, 8));
        System.out.println(firstAtLeast(values, 20));
    }
}
