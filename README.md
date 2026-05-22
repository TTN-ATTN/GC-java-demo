# Java GC Memory Demo

Demo nay tao ap luc bo nho de quan sat Heap, Stack va Garbage Collector.
Java code chi tao hien tuong; shell script lo viec log va thong ke.

## Demo truc tiep tren terminal

Lenh nen dung khi thuyet trinh:

```bash
./scripts/terminal-demo.sh all g1 128
```

Script se:

- Compile `MemoryGcDemo`.
- Chay JVM voi `-Xms128m -Xmx128m -Xss256k`.
- Ghi GC log bang `-Xlog:gc*`.
- Ghi thong ke Young/Old/Metaspace/GC count bang `jstat -gcutil`.
- Tinh nhanh tong so GC pause va tong pause time.

Tham so:

- `all|churn|promotion|leak|stack`: kich ban demo.
- `default|serial|parallel|g1|zgc|shenandoah`: loai GC.
- `128`: heap size MB.

Mot vai lenh rieng neu muon tu go:

```bash
java -Xms128m -Xmx128m -Xss256k -XX:+UseG1GC \
  -Xlog:gc*,gc+heap=debug:file=out/gc.log:uptime,level,tags \
  -cp out/classes MemoryGcDemo all

jstat -gcutil <PID> 1000
grep -E 'Pause|Full|Concurrent|Heap' out/gc.log
```

## Chay demo

```bash
./scripts/run-demo.sh all g1 128
```

Tham so:

- `all|churn|promotion|leak|stack`: kich ban demo.
- `default|serial|parallel|g1|zgc|shenandoah`: loai GC.
- `128`: heap size MB, script se dung `-Xms128m -Xmx128m`.

Vi du so sanh GC:

```bash
./scripts/run-demo.sh all serial 128
./scripts/run-demo.sh all parallel 128
./scripts/run-demo.sh all g1 128
./scripts/run-demo.sh all zgc 128
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

- `churn`: tao nhieu array song ngan, heap tang roi giam sau GC.
- `promotion`: giu lai mot phan array trong list, old generation tang dan.
- `leak`: list reachable tang dan, GC khong don duoc object con reference.
- `stack`: de quy sau voi `-Xss256k`, gay `StackOverflowError`.
