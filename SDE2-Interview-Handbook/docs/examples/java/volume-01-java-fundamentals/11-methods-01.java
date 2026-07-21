public class MethodsDemo {
    public static void append(StringBuilder builder, String suffix) {
        builder.append(suffix);
    }

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder("Hello");
        append(sb, " World");
        System.out.println(sb);
    }
}
