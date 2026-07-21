public class StringPatterns {
    public static String reverseWords(String s) {
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isEmpty()) {
                sb.append(parts[i]);
                if (i > 0) sb.append(' ');
            }
        }
        return sb.toString();
    }
}
