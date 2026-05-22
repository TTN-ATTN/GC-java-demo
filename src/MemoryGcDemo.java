import java.util.ArrayList;
import java.util.List;

public class MemoryGcDemo {
    // Các đối tượng được giữ ở đây sẽ sống sót qua Young GC và có thể được thăng cấp lên Old Gen.
    private static final List<byte[]> SURVIVORS = new ArrayList<>();

    // Danh sách này không bao giờ được xóa, nên nó mô phỏng một rò rỉ bộ nhớ còn tham chiếu được.
    private static final List<byte[]> LEAK = new ArrayList<>();

    // Ngăn JVM tối ưu hóa phần cấp phát và công việc đệ quy của demo.
    private static volatile int sink;
    private static volatile int stackDepth;
    private static boolean printedStackSample;

    public static void main(String[] args) throws Exception {
        String scenario = args.length == 0 ? "all" : args[0];

        System.out.println("PID=" + ProcessHandle.current().pid());
        System.out.println("scenario=" + scenario);

        if ("all".equals(scenario) || "churn".equals(scenario)) {
            heapChurn();
            requestGc("after churn");
        }

        if ("all".equals(scenario) || "promotion".equals(scenario)) {
            objectPromotion();
            SURVIVORS.clear();
            requestGc("after clearing survivors");
        }

        if ("all".equals(scenario) || "leak".equals(scenario)) {
            reachableLeak();
            requestGc("leak objects are still reachable");
        }

        if ("all".equals(scenario) || "stack".equals(scenario)) {
            stackOverflowDemo();
        }

        System.out.println("done sink=" + sink);
        Thread.sleep(2_000);
    }

    private static void heapChurn() throws InterruptedException {
        System.out.println("phase=heap-churn short-lived objects");

        for (int round = 1; round <= 12; round++) {
            // Những mảng này được cấp phát trên Heap, chủ yếu ở Eden/Young Gen.
            List<byte[]> batch = new ArrayList<>(20_000);

            for (int i = 0; i < 20_000; i++) {
                batch.add(newArray(1_024));
            }

            System.out.println("churn round=" + round + " allocated about 20MB");

            // Sau khi clear(), các mảng không còn tham chiếu và trở thành ứng viên cho GC.
            batch.clear();
            Thread.sleep(500);
        }
    }

    private static void objectPromotion() throws InterruptedException {
        System.out.println("phase=promotion keep some objects alive");

        byte[][] shortWindow = new byte[4_096][];

        for (int round = 1; round <= 12; round++) {
            for (int i = 0; i < 25_000; i++) {
                // Cửa sổ ngắn giữ một số đối tượng gần đây còn sống tạm thời.
                shortWindow[i % shortWindow.length] = newArray(1_024);

                if (i % 1_000 == 0) {
                    // Các tham chiếu sống lâu khiến những đối tượng này sống sót qua các chu kỳ GC.
                    SURVIVORS.add(newArray(64 * 1_024));
                }
            }

            System.out.println("promotion round=" + round + " survivors=" + SURVIVORS.size());
            Thread.sleep(500);
        }
    }

    private static void reachableLeak() throws InterruptedException {
        System.out.println("phase=reachable-leak objects remain reachable");

        for (int round = 1; round <= 12; round++) {
            for (int i = 0; i < 12; i++) {
                // Vì LEAK là static và không được xóa, GC phải giữ các đối tượng này.
                LEAK.add(newArray(256 * 1_024));
            }

            System.out.println("leak round=" + round + " retainedObjects=" + LEAK.size());
            Thread.sleep(500);
        }
    }

    private static void stackOverflowDemo() {
        System.out.println("phase=stack recursion");

        try {
            recurse(1);
        } catch (StackOverflowError error) {
            System.out.println("StackOverflowError depth=" + stackDepth);
        }
    }

    private static int recurse(int depth) {
        stackDepth = depth;

        // Các biến cục bộ tồn tại trong stack frame hiện tại.
        int localA = depth;
        int localB = localA * 31;
        int localC = localB ^ 0x5a5a5a5a;
        sink ^= localC;

        if (!printedStackSample && depth >= 250) {
            printedStackSample = true;
            // Hiển thị các frame recurse() lặp lại trước StackOverflowError cuối cùng.
            printStackSample();
            sleepQuietly(4_000);
        }

        // Mỗi lần gọi đệ quy sẽ thêm một stack frame nữa.
        return recurse(depth + 1) + localC;
    }

    private static byte[] newArray(int bytes) {
        // byte[] là một đối tượng, nên mỗi lần gọi đều cấp phát trên Heap.
        byte[] data = new byte[bytes];
        data[0] = (byte) bytes;
        data[data.length - 1] = (byte) (bytes >>> 8);
        sink ^= data[0] ^ data[data.length - 1];
        return data;
    }

    private static void requestGc(String label) throws InterruptedException {
        System.out.println("request-gc=" + label);
        // Lệnh này yêu cầu Full GC; hữu ích cho demo, không khuyến nghị trong ứng dụng thực.
        System.gc();
        Thread.sleep(2_000);
    }

    private static void printStackSample() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        System.out.println("stack-sample-depth=" + stackDepth);

        int limit = Math.min(trace.length, 20);
        for (int i = 2; i < limit; i++) {
            System.out.println("  " + trace[i]);
        }

        if (trace.length > limit) {
            System.out.println("  ... " + (trace.length - limit) + " more frames");
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
