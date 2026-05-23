import java.util.ArrayList;
import java.util.List;

public class MemoryGcDemo {
    private static final List<byte[]> LEAK = new ArrayList<>();
    private static volatile int sink;

    public static void main(String[] args) throws Exception {
        String scenario = args.length == 0 ? "churn" : args[0];
        System.out.println("PID=" + ProcessHandle.current().pid());
        System.out.println("scenario=" + scenario);

        if ("churn".equals(scenario)) {
            churnDemo();
        } else if ("leak".equals(scenario)) {
            leakDemo();
        } else {
            System.out.println("Scenario không hợp lệ: " + scenario);
            System.out.println("Cách chạy: ./run.sh [churn|leak] [heapMB]");
            return;
        }

        System.out.println("Hoàn thành.");
        Thread.sleep(2_000);
    }

    private static void churnDemo() throws InterruptedException {
        System.out.println("Bước 1: Cấp phát đối tượng trên heap");
        List<byte[]> temp = new ArrayList<>();
        for (int batch = 1; batch <= 4; batch++) {
            for (int i = 0; i < 20_000; i++) {
                temp.add(allocateArray());
            }
            System.out.println("Batch " + batch + ": đã cấp phát khoảng 20MB");
            Thread.sleep(500);
        }

        System.out.println("Bước 2: Bỏ tham chiếu");
        temp.clear(); // Xóa reference trong list; các byte[] không còn reachable nên GC có thể thu gom.
        temp = null;

        System.out.println("Bước 3: Yêu cầu GC");
        System.gc(); // Đây chỉ là yêu cầu; JVM quyết định khi nào/cách chạy GC.
        Thread.sleep(2_000);
    }

    private static void leakDemo() throws InterruptedException {
        System.out.println("Bước 1: Cấp phát đối tượng và giữ tham chiếu");
        for (int i = 0; i < 120; i++) {
            LEAK.add(new byte[256 * 1024]); // Vẫn còn truy cập được thông qua LEAK.
            Thread.sleep(30);
        }

        System.out.println("Bước 2: Yêu cầu GC khi tham chiếu vẫn tồn tại");
        System.gc();
        Thread.sleep(2_000);

        System.out.println("Quan sát: các đối tượng trong LEAK vẫn reachable, nên GC không thể giải phóng chúng.");
        sink = LEAK.size();
    }

    private static byte[] allocateArray() {
        byte[] data = new byte[1024]; // new byte[] tạo một đối tượng trên heap.
        data[0] = 1;
        sink ^= data[0]; // Ngăn phần xử lý nhỏ này bị tối ưu hóa bỏ qua.
        return data;
    }
}
