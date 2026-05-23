# Note doc logs demo GC

Chay demo:

```bash
./scripts/run.sh
```

Neu dang o trong thu muc `scripts/`:

```bash
./run.sh
```

Script se in `output=...`. Do la thu muc chua log cua lan chay hien tai.
Script khong ep loai GC nao. JVM se dung GC mac dinh. Tren may Java 21 cua ban,
`java -Xlog:gc -version` cho thay GC mac dinh la `Using G1`.

## app.log

File nay la log do Java in ra, dung de biet chuong trinh dang o phase nao.

- `Step 1: Allocate objects on heap`: tao nhieu object bang `new byte[]`.
- `Step 2: Drop references`: goi `temp.clear()`, object khong con reachable.
- `Step 3: Request GC`: goi `System.gc()` de yeu cau JVM chay GC.
- `Observe: GC pause appears in gc.log`: mo `gc.log` de thay pause va heap truoc/sau GC.
- Scenario phu `leak`: object van nam trong static list `LEAK`, nen GC khong don duoc.

## jstat.log

File nay la thong ke moi giay tu lenh `jstat -gcutil`.

Cot quan trong:

- `E`: Eden usage %. Object moi thuong duoc cap phat o day.
- `S0`, `S1`: Survivor space usage %.
- `O`: Old Gen usage %. Tang khi object song lau hoac leak.
- `M`: Metaspace usage %.
- `YGC`: so lan Young GC.
- `YGCT`: tong thoi gian Young GC, don vi giay.
- `FGC`: so lan Full GC.
- `FGCT`: tong thoi gian Full GC, don vi giay.
- `GCT`: tong thoi gian GC, don vi giay.

Cach noi:

- Khi churn demo chay, `E` tang: object moi dang duoc cap phat tren Heap/Eden.
- Sau `System.gc()`, `FGC` hoac `GCT` co the tang: JVM da dung chuong trinh de GC.
- Neu chay `./run.sh leak 128`, sau GC heap van cao hon vi object van reachable.

## gc.log

File nay la GC log chi tiet cua JVM.

Tim cac dong:

```text
Using G1
Pause Young
Pause Full
77M->10M(128M)
49M->37M(128M)
```

Cach doc `77M->10M(128M)`:

- `77M`: heap used truoc GC.
- `10M`: heap used sau GC.
- `128M`: max heap da set bang `-Xmx128m`.

`Pause Young` thuong xuat hien khi Eden day. `Pause Full` xuat hien khi script goi `System.gc()` hoac JVM can don manh hon.

## gc-summary.log

File nay la ban rut gon tu `gc.log`, chi giu cac dong de thuyet trinh.

Cuoi script co:

- `pause_count`: so lan GC pause.
- `pause_total_ms`: tong thoi gian pause tinh bang millisecond.

Dung de ket luan nhanh: trong ca demo JVM da dung ung dung bao nhieu lan va tong thoi gian dung la bao lau.

## Cau noi ngan khi thuyet trinh

Demo nay cho thay:

1. Object tao bang `new byte[]` nam tren Heap.
2. `temp.clear()` lam object khong con reachable.
3. `System.gc()` chi la request, JVM quyet dinh cach chay GC.
4. Object van reachable thi GC khong don duoc, day la y tuong memory leak.
