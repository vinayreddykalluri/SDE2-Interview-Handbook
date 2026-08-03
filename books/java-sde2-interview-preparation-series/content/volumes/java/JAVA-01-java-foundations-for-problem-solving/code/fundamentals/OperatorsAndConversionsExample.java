public final class OperatorsAndConversionsExample {
    public static void main(String[] args) {
        int maximum = Integer.MAX_VALUE;
        long correct = (long) maximum + 1;
        double average = (8 + 9) / 2.0;
        String text = null;
        boolean safe = text != null && !text.isEmpty();

        System.out.println(correct);
        System.out.println(average);
        System.out.println(safe);
    }
}
