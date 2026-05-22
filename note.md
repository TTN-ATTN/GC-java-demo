# Note doc logs demo GC

Chay demo:

```bash
./scripts/run.sh all g1 128
```

Neu dang o trong thu muc `scripts/`:

```bash
./run.sh all g1 128
```

Script se in `output=...`. Do la thu muc chua log cua lan chay hien tai.

## app.log

File nay la log do Java in ra, dung de biet chuong trinh dang o phase nao.

- `phase=heap-churn`: tao nhieu object ngan han, de thay Eden tang va Young GC chay.
- `phase=promotion`: giu lai mot phan object trong `SURVIVORS`, de thay Old Gen tang.
- `phase=reachable-leak`: giu object trong static list `LEAK`, GC khong don duoc vi van reachable.
- `phase=stack recursion`: goi de quy sau de tao nhieu stack frame.
- `StackOverflowError depth=...`: stack day, khong phai loi Heap.

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

- Khi `heap-churn`, `E` tang roi `YGC` tang: object ngan han duoc don o Young Gen.
- Khi `promotion`, `O` tang: object song qua nhieu GC bi day len Old Gen.
- Khi `leak`, sau Full GC ma `O` van cao: object van reachable nen GC khong the don.

## gc.log

File nay la GC log chi tiet cua JVM.

Tim cac dong:

```text
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
2. Object ngan han lam Eden tang va kich hoat Young GC.
3. Object duoc giu reference lau hon co the lam Old Gen tang.
4. Object van reachable thi GC khong don duoc, day la y tuong memory leak.
5. De quy sau tao nhieu stack frame va gay `StackOverflowError`.
