import java.util.ArrayList;
import java.util.List;

public class GcExample {
    public static void main(String[] args) throws InterruptedException {
        List<byte[]> allocations = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            allocations.add(new byte[1024 * 1024]);
            if (i % 50 == 0) allocations.clear();
            Thread.sleep(5);
        }
    }
}
