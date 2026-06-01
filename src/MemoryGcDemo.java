import java.util.ArrayList;
import java.util.List;

public class MemoryGcDemo {
    private static final int WAIT_MS = 2_000;
    private static volatile int sink;

    public static void main(String[] args) throws Exception {
        String scenario = args.length == 0 ? "churn" : args[0];
        System.out.println("PID=" + ProcessHandle.current().pid());
        System.out.println("scenario=" + scenario);

        if ("churn".equals(scenario)) {
            churnDemo();
        } else if ("leak".equals(scenario)) {
            leakDemo();
        } else if ("oom".equals(scenario)) {
            oomDemo();
        } else {
            System.out.println("Scenario không hợp lệ: " + scenario);
            return;
        }

        System.out.println("Hoàn thành.");
        Thread.sleep(2_000);
    }

    private static void churnDemo() throws InterruptedException {
        System.out.println("Bước 1: Cấp phát đối tượng trên heap");
        List<byte[]> churn = new ArrayList<>();
        for (int batch = 1; batch <= 4; batch++) {
            for (int i = 0; i < 20_000; i++) {
                churn.add(allocateArray());
            }
            System.out.println("Batch " + batch + ": đã cấp phát khoảng 20MB");
            Thread.sleep(700);
        }

        System.out.println("Checkpoint: heap vẫn giữ reference tới " + churn.size() + " object");
        Thread.sleep(WAIT_MS);

        System.out.println("Bước 2: Bỏ tham chiếu");
        churn.clear(); // Xóa reference trong list; các byte[] không còn reachable nên GC có thể thu gom.
        churn = null;
        System.out.println("Checkpoint: object đã unreachable, nhưng bộ nhớ có thể chưa giảm cho tới khi GC chạy");
        Thread.sleep(WAIT_MS);

        System.out.println("Bước 3: Yêu cầu GC");
        System.gc(); // Đây chỉ là yêu cầu; JVM quyết định khi nào/cách chạy GC.
        Thread.sleep(3_000);
        System.out.println("churn size sau GC: " + (churn == null ? "0" : churn.size()) + " objects");
    }

    private static void leakDemo() throws InterruptedException {
        List<byte[]> leak = new ArrayList<>();
        System.out.println("Bước 1: Cấp phát đối tượng và giữ tham chiếu");
        for (int batch = 1; batch <= 4; batch++) {
            for (int i = 0; i < 20_000; i++) {
                leak.add(allocateArray());
            }
            System.out.println("Batch " + batch + ": đã cấp phát khoảng 20MB");
            Thread.sleep(700);
        }

        System.out.println("Checkpoint: LEAK vẫn giữ reference tới " + leak.size() + " object");
        Thread.sleep(WAIT_MS);

        System.out.println("Bước 2: Yêu cầu GC khi tham chiếu vẫn tồn tại");
        System.gc(); // Object còn reachable nên GC không được phép thu gom chúng.
        Thread.sleep(3_000);

        System.out.println("Quan sát: các đối tượng trong leak vẫn reachable, nên GC không thể giải phóng chúng.");
        System.out.println("leak size sau GC: " + leak.size() + " objects");
    }

    private static void oomDemo() throws InterruptedException {
        List<byte[]> heap = new ArrayList<>();
        Runtime runtime = Runtime.getRuntime();

        System.out.println("Bước 1: Cấp phát liên tục và không bỏ reference");
        for (int batch = 1; ; batch++) {
            for (int i = 0; i < 20_000; i++) {
                heap.add(allocateArray());
            }
            long used = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf(
                    "Batch %d: giữ %,d object | heap used=%dMB / max=%dMB%n",
                    batch,
                    heap.size(),
                    toMb(used),
                    toMb(runtime.maxMemory())
            );
            Thread.sleep(700);
        }
    }

    /*
     * new byte[] tạo một đối tượng trên heap.
     * Việc trả về nó cho phép churn giữ tham chiếu cho đến khi churn.clear().
     */
    private static byte[] allocateArray() {
        byte[] data = new byte[1024]; // new byte[] tạo một đối tượng trên heap.
        data[0] = 1;
        sink ^= data[0];
        return data;
    }

    private static long toMb(long bytes) {
        return bytes / (1024 * 1024);
    }
}
