# Ghi chú: nhật ký demo GC

Chạy demo:

```bash
./scripts/run.sh
```

Nếu đang ở trong thư mục `scripts/`:

```bash
./run.sh
```

Script sẽ in `output=...`. Đó là thư mục chứa log của lần chạy hiện tại.
Script không ép loại GC nào. JVM sẽ dùng GC mặc định. Trên máy Java 21 của bạn,
`java -Xlog:gc -version` cho thấy GC mặc định là `Using G1`.

## app.log

File này là log do Java in ra, dùng để biết chương trình đang ở pha nào.

- `Step 1: Allocate objects on heap`: tạo nhiều object bằng `new byte[]`.
- `Step 2: Drop references`: gọi `temp.clear()`, object không còn reachable.
- `Step 3: Request GC`: gọi `System.gc()` để yêu cầu JVM chạy GC.
- `Observe: GC pause appears in gc.log`: mở `gc.log` để thấy pause và heap trước/sau GC.
- Kịch bản phụ `leak`: object vẫn nằm trong static list `LEAK`, nên GC không dọn được.

## jstat.log

| Cột    | Tên đầy đủ             | Ý nghĩa                                                                                                                          |
| ------ | ---------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| `S0`   | Survivor Space 0       | Tỉ lệ sử dụng của vùng Survivor 0 trong Young Generation. Đây là nơi lưu các object còn sống sau Young GC.                       |
| `S1`   | Survivor Space 1       | Tỉ lệ sử dụng của vùng Survivor 1 trong Young Generation. JVM thường luân phiên sử dụng S0 và S1 để chứa object sống sót sau GC. |
| `E`    | Eden Space             | Tỉ lệ sử dụng của vùng Eden. Đây là nơi hầu hết object mới được cấp phát khi dùng `new`.                                         |
| `O`    | Old Generation         | Tỉ lệ sử dụng của vùng Old Generation. Các object sống lâu hoặc sống sót qua nhiều lần GC có thể được chuyển vào vùng này.       |
| `M`    | Metaspace              | Tỉ lệ sử dụng của Metaspace. Vùng này lưu metadata của class, method, runtime information,...                                    |
| `CCS`  | Compressed Class Space | Tỉ lệ sử dụng của vùng lưu thông tin class khi JVM bật Compressed Class Pointers.                                                |
| `YGC`  | Young GC Count         | Số lần Young GC đã xảy ra. Young GC chủ yếu dọn vùng Young Generation, đặc biệt là Eden.                                         |
| `YGCT` | Young GC Time          | Tổng thời gian đã dùng cho Young GC, tính bằng giây.                                                                             |
| `FGC`  | Full GC Count          | Số lần Full GC đã xảy ra. Full GC thường dọn phạm vi rộng hơn, bao gồm Old Generation và có thể gây pause đáng chú ý.            |
| `FGCT` | Full GC Time           | Tổng thời gian đã dùng cho Full GC, tính bằng giây.                                                                              |
| `CGC`  | Concurrent GC Count    | Số lần Concurrent GC đã xảy ra. Đây là các chu kỳ GC chạy đồng thời với chương trình, thường thấy ở các GC hiện đại như G1.      |
| `CGCT` | Concurrent GC Time     | Tổng thời gian đã dùng cho Concurrent GC, tính bằng giây.                                                                        |
| `GCT`  | Total GC Time          | Tổng thời gian JVM đã dùng cho tất cả hoạt động GC. Thường xấp xỉ `YGCT + FGCT + CGCT`.                                          |


## gc.log

File này là GC log chi tiết của JVM.

Tìm các dòng:

```text
Using G1
Pause Young
Pause Full
77M->10M(128M)
49M->37M(128M)
```

Cách đọc `77M->10M(128M)`:

- `77M`: heap đang dùng trước GC.
- `10M`: heap đang dùng sau GC.
- `128M`: kích thước tối đa heap được đặt bằng `-Xmx128m`.

`Pause Young` thường xuất hiện khi Eden đầy. `Pause Full` xuất hiện khi script gọi `System.gc()` hoặc JVM cần dọn mạnh hơn.

## gc-summary.log

File này là bản rút gọn từ `gc.log`, chỉ giữ các dòng để thuyết trình.

Cuối script có:

- `pause_count`: số lần GC pause.
- `pause_total_ms`: tổng thời gian pause tính bằng millisecond.

Dùng để kết luận nhanh: trong ca demo JVM đã dừng ứng dụng bao nhiêu lần và tổng thời gian dừng là bao lâu.

## Câu nói ngắn khi thuyết trình

Demo này cho thấy:

1. Object tạo bằng `new byte[]` nằm trên Heap.
2. `temp.clear()` làm object không còn reachable.
3. `System.gc()` chỉ là một yêu cầu, JVM quyết định cách chạy GC.
4. Object vẫn reachable thì GC không dọn được — đó là ý tưởng về memory leak.
