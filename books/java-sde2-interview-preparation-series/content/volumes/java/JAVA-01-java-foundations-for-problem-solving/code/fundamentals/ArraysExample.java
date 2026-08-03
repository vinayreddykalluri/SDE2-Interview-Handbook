import java.util.Arrays;

public final class ArraysExample {
    static int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    public static void main(String[] args) {
        int[] values = {4, 1, 3};
        int[] copy = values.clone();
        Arrays.sort(copy);

        System.out.println(sum(values));
        System.out.println(Arrays.toString(values));
        System.out.println(Arrays.toString(copy));
    }
}
