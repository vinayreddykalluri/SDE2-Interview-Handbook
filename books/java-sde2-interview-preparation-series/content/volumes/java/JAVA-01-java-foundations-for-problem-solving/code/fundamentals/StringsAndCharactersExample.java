public final class StringsAndCharactersExample {
    static String reverseAscii(String input) {
        StringBuilder result = new StringBuilder(input.length());
        for (int index = input.length() - 1; index >= 0; index--) {
            result.append(input.charAt(index));
        }
        return result.toString();
    }

    public static void main(String[] args) {
        String first = new String("java");
        String second = new String("java");
        char character = '7';

        System.out.println(first == second);
        System.out.println(first.equals(second));
        System.out.println(character - '0');
        System.out.println(reverseAscii("SDE2"));
    }
}
