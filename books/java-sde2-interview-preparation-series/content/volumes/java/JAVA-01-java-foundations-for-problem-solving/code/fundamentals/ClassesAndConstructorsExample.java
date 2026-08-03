public final class ClassesAndConstructorsExample {
    static final class Counter {
        private static int created;
        private int value;

        Counter() {
            this(0);
        }

        Counter(int initialValue) {
            if (initialValue < 0) {
                throw new IllegalArgumentException("negative initial value");
            }
            value = initialValue;
            created++;
        }

        void increment() {
            value++;
        }

        int value() {
            return value;
        }

        static int created() {
            return created;
        }
    }

    public static void main(String[] args) {
        Counter first = new Counter();
        Counter second = new Counter(4);
        second.increment();

        System.out.println(first.value());
        System.out.println(second.value());
        System.out.println(Counter.created());
    }
}
