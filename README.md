# Java GC Memory Demo

Demo này tạo áp lực bộ nhớ để quan sát Heap, Stack và Garbage Collector.
Mã Java chỉ tạo hiện tượng; shell script lo việc ghi log và thống kê.

## Demo trực tiếp trên terminal

Lệnh nên dùng khi thuyết trình:

```bash
./scripts/run.sh
```

Script sẽ:

- Biên dịch `MemoryGcDemo`.
- Chạy JVM với `-Xms128m -Xmx128m`.
- Dùng GC mặc định của JVM. Trên Java 21 của máy bạn là G1.
- Ghi GC log bằng `-Xlog:gc*`.
- Ghi thống kê Young/Old/Metaspace/GC count bằng `jstat -gcutil`.
- Tính nhanh tổng số GC pause và tổng pause time.

Tham số:

- `churn|leak`: kịch bản demo. Mặc định là `churn`.
- `128`: kích thước heap tính bằng MB.

Một vài lệnh riêng nếu muốn tự gõ:

```bash
java -Xms128m -Xmx128m \
  -Xlog:gc*,gc+heap=debug:file=out/gc.log:uptime,level,tags \
  -cp out/classes MemoryGcDemo churn

jstat -gcutil <PID> 1000
grep -E 'Pause|Full|Concurrent|Heap' out/gc.log
```

## Chạy demo

```bash
./scripts/run.sh churn 128
```

Tham số:

- `churn|leak`: kịch bản demo. Mặc định là `churn`.
- `128`: kích thước heap tính bằng MB, script sẽ dùng `-Xms128m -Xmx128m`.

Ví dụ chạy từng phase:

```bash
./scripts/run.sh churn 128
./scripts/run.sh leak 128
```

Mỗi lần chạy sẽ tạo thư mục `out/<timestamp>-<scenario>-<gc>-<heap>m/` gồm:

- `gc.log`: log GC thô từ JVM với `-Xlog:gc*`.
- `jstat.log`: thống kê heap/GC mỗi giây.
- `app.log`: marker phase do Java in ra.
- `gc-summary.log`: các dòng GC quan trọng đã lọc sẵn.

## Quan sát trực tiếp

Khi chương trình đang chạy, terminal sẽ in PID. Có thể dùng:

```bash
jstat -gcutil <PID> 1000
jconsole <PID>
```

`jstat` hiển thị Young/Old/Metaspace/GC count mỗi giây. `jconsole` hiển thị biểu đồ heap
trực quan hơn.

## Ý tưởng trình bày

- `churn`: tạo nhiều array trên Heap, xóa reference, yêu cầu GC.
- `leak`: giữ array trong static list, GC không dọn được object còn reference.
