# Java GC Memory Demo

Demo nay tao ap luc bo nho de quan sat Heap, Stack va Garbage Collector.
Java code chi tao hien tuong; shell script lo viec log va thong ke.

## Demo truc tiep tren terminal

Lenh nen dung khi thuyet trinh:

```bash
./scripts/run.sh
```

Script se:

- Compile `MemoryGcDemo`.
- Chay JVM voi `-Xms128m -Xmx128m`.
- Dung GC mac dinh cua JVM. Tren Java 21 cua may ban la G1.
- Ghi GC log bang `-Xlog:gc*`.
- Ghi thong ke Young/Old/Metaspace/GC count bang `jstat -gcutil`.
- Tinh nhanh tong so GC pause va tong pause time.

Tham so:

- `churn|leak`: kich ban demo. Mac dinh la `churn`.
- `128`: heap size MB.

Mot vai lenh rieng neu muon tu go:

```bash
java -Xms128m -Xmx128m \
  -Xlog:gc*,gc+heap=debug:file=out/gc.log:uptime,level,tags \
  -cp out/classes MemoryGcDemo churn

jstat -gcutil <PID> 1000
grep -E 'Pause|Full|Concurrent|Heap' out/gc.log
```

## Chay demo

```bash
./scripts/run.sh churn 128
```

Tham so:

- `churn|leak`: kich ban demo. Mac dinh la `churn`.
- `128`: heap size MB, script se dung `-Xms128m -Xmx128m`.

Vi du chay tung phase:

```bash
./scripts/run.sh churn 128
./scripts/run.sh leak 128
```

Moi lan chay se tao thu muc `out/<timestamp>-<scenario>-<gc>-<heap>m/` gom:

- `gc.log`: log GC that tu JVM voi `-Xlog:gc*`.
- `jstat.log`: thong ke heap/GC moi giay.
- `app.log`: marker phase do Java in ra.
- `gc-summary.log`: cac dong GC quan trong da loc san.

## Quan sat truc tiep

Khi chuong trinh dang chay, terminal se in PID. Co the dung:

```bash
jstat -gcutil <PID> 1000
jconsole <PID>
```

`jstat` hien Young/Old/Metaspace/GC count moi giay. `jconsole` hien bieu do heap
truc quan hon.

## Y tuong trinh bay

- `churn`: tao nhieu array tren Heap, clear reference, request GC.
- `leak`: giu array trong static list, GC khong don duoc object con reference.
