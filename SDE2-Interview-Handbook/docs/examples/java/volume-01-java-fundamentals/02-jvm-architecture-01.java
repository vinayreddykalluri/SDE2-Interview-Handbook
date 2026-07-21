public class JvmStats {
    public static void main(String[] args) {
        System.out.println("Runtime processors: " + Runtime.getRuntime().availableProcessors());
        long max = Runtime.getRuntime().maxMemory();
        System.out.println("Max memory bytes: " + max);
    }
}
