import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Executable model of the arithmetic and algorithms in the HLD case book.
 *
 * Every number quoted in the chapters is printed by this program, and every
 * claim that can be checked is checked. Randomized experiments use fixed
 * seeds, so the output is identical on every run. Run it with:
 *
 *     javac --release 21 HighLevelDesignModel.java && java -ea HighLevelDesignModel
 *
 * Sections map to chapters:
 *   1. Estimation and key space for a URL shortener                  (ch 2)
 *   2. Consistent hashing: balance and remapping                     (ch 3)
 *   3. Feed fan-out on a power-law follower graph                    (ch 4)
 *   4. Chat ordering, gap detection, and presence load               (ch 5)
 *   5. Bloom filters: sizing and measured false positives            (ch 6)
 *   6. Geohash cells and the boundary problem                        (ch 7)
 *   7. Idempotent payments, double-entry ledger, seat contention     (ch 8)
 *   8. Storage durability and time-series volume                     (ch 9)
 */
public final class HighLevelDesignModel {

    private HighLevelDesignModel() {}

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("check failed: " + message);
        }
    }

    static final long SECONDS_PER_DAY = 86_400;
    static final long SECONDS_PER_MONTH = 30 * SECONDS_PER_DAY;

    /** SplitMix64 finalizer: a fast, well-mixed 64-bit hash of a 64-bit input. */
    static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    static long hash(String key) {
        long h = 1125899906842597L;
        for (int i = 0; i < key.length(); i++) {
            h = 31 * h + key.charAt(i);
        }
        return mix64(h);
    }

    // ------------------------------------------------------------------
    // 1. URL shortener: estimation and key space
    // ------------------------------------------------------------------

    static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    static String toBase62(long value) {
        if (value == 0) {
            return "0";
        }
        StringBuilder out = new StringBuilder();
        long v = value;
        while (v > 0) {
            out.append(BASE62.charAt((int) (v % 62)));
            v /= 62;
        }
        return out.reverse().toString();
    }

    static long fromBase62(String text) {
        long v = 0;
        for (int i = 0; i < text.length(); i++) {
            v = v * 62 + BASE62.indexOf(text.charAt(i));
        }
        return v;
    }

    static void urlShortener() {
        System.out.println("== 1. URL shortener ==");
        long newPerMonth = 100_000_000L;
        int readsPerWrite = 100;
        int bytesPerRecord = 500;
        int years = 5;

        double writesPerSecond = (double) newPerMonth / SECONDS_PER_MONTH;
        double readsPerSecond = writesPerSecond * readsPerWrite;
        long totalRecords = newPerMonth * 12 * years;
        double storageTb = (double) totalRecords * bytesPerRecord / 1e12;
        System.out.printf(
                "  writes/s %.1f average, reads/s %,.0f average%n",
                writesPerSecond, readsPerSecond);
        System.out.printf(
                "  %d years -> %,d records, %.1f TB at %d bytes each%n",
                years, totalRecords, storageTb, bytesPerRecord);
        check(Math.abs(writesPerSecond - 38.58) < 0.01, "write rate");
        check(totalRecords == 6_000_000_000L, "record count");

        double keySpace7 = Math.pow(62, 7);
        double counterYears = keySpace7 / writesPerSecond / (365.0 * SECONDS_PER_DAY);
        System.out.printf(
                "  62^7 = %.3e keys; a counter lasts %,.0f years at this rate%n",
                keySpace7, counterYears);

        double expectedCollisions = (double) totalRecords * totalRecords / (2 * keySpace7);
        System.out.printf(
                "  random 7-char keys for %,d URLs: ~%,.0f expected collisions%n",
                totalRecords, expectedCollisions);
        check(expectedCollisions > 1e6, "truncated random keys must handle collisions");

        int smallSpace = 62 * 62 * 62;
        int draws = 2_000;
        int trials = 400;
        double predicted = (double) draws * (draws - 1) / (2.0 * smallSpace);
        SplittableRandom random = new SplittableRandom(17);
        long observed = 0;
        for (int trial = 0; trial < trials; trial++) {
            Map<Integer, Integer> seen = new HashMap<>();
            for (int i = 0; i < draws; i++) {
                int key = random.nextInt(smallSpace);
                int before = seen.merge(key, 1, Integer::sum) - 1;
                observed += before;
            }
        }
        double observedMean = (double) observed / trials;
        System.out.printf(
                "  birthday check, %,d keys into 62^3: predicted %.2f colliding pairs,"
                        + " measured %.2f over %d trials%n",
                draws, predicted, observedMean, trials);
        check(Math.abs(observedMean - predicted) / predicted < 0.05, "birthday estimate holds");

        long id = 125_000_000_000L;
        String code = toBase62(id);
        System.out.printf(
                "  counter id %,d -> base62 \"%s\" (%d chars)%n", id, code, code.length());
        check(fromBase62(code) == id && code.length() == 7, "base62 round trip");
    }

    // ------------------------------------------------------------------
    // 2. Consistent hashing
    // ------------------------------------------------------------------

    static final class HashRing {
        private final NavigableMap<Long, String> ring = new TreeMap<>();
        private final int virtualNodes;

        HashRing(int virtualNodes) {
            this.virtualNodes = virtualNodes;
        }

        void add(String node) {
            for (int v = 0; v < virtualNodes; v++) {
                ring.put(hash(node + "#" + v), node);
            }
        }

        String owner(String key) {
            Map.Entry<Long, String> e = ring.ceilingEntry(hash(key));
            return e != null ? e.getValue() : ring.firstEntry().getValue();
        }
    }

    static double maxOverMean(Map<String, Integer> load) {
        double mean = load.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        int max = load.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return max / mean;
    }

    static void consistentHashing() {
        System.out.println();
        System.out.println("== 2. Consistent hashing: 10 nodes, 1,000,000 keys ==");
        int nodes = 10;
        int keys = 1_000_000;
        double[] expectedBands = {Double.MAX_VALUE, Double.MAX_VALUE, 1.30, 1.15};
        int[] vnodeCounts = {1, 10, 100, 200};
        for (int i = 0; i < vnodeCounts.length; i++) {
            HashRing ring = new HashRing(vnodeCounts[i]);
            for (int n = 0; n < nodes; n++) {
                ring.add("node-" + n);
            }
            Map<String, Integer> load = new HashMap<>();
            for (int k = 0; k < keys; k++) {
                load.merge(ring.owner("key-" + k), 1, Integer::sum);
            }
            double ratio = maxOverMean(load);
            System.out.printf(
                    "  %3d virtual nodes each: busiest node holds %.2fx the mean%n",
                    vnodeCounts[i], ratio);
            check(ratio < expectedBands[i], "balance improves with virtual nodes");
        }

        HashRing before = new HashRing(100);
        HashRing after = new HashRing(100);
        for (int n = 0; n < nodes; n++) {
            before.add("node-" + n);
            after.add("node-" + n);
        }
        after.add("node-" + nodes);
        int ringMoved = 0;
        int moduloMoved = 0;
        for (int k = 0; k < keys; k++) {
            String key = "key-" + k;
            if (!before.owner(key).equals(after.owner(key))) {
                ringMoved++;
            }
            long h = hash(key) & Long.MAX_VALUE;
            if (h % nodes != h % (nodes + 1)) {
                moduloMoved++;
            }
        }
        double ringFraction = (double) ringMoved / keys;
        double moduloFraction = (double) moduloMoved / keys;
        System.out.printf(
                "  add an 11th node: ring moves %.1f%% of keys, hash mod N moves %.1f%%%n",
                ringFraction * 100, moduloFraction * 100);
        System.out.printf("  ideal is 1/11 = %.1f%%%n", 100.0 / (nodes + 1));
        check(Math.abs(ringFraction - 1.0 / (nodes + 1)) < 0.02, "ring moves about 1/(N+1)");
        check(moduloFraction > 0.85, "modulo moves almost everything");
    }

    // ------------------------------------------------------------------
    // 3. Feed fan-out
    // ------------------------------------------------------------------

    static void feedFanOut() {
        System.out.println();
        System.out.println("== 3. Feed fan-out: 1,000,000 accounts, Zipf followers ==");
        int accounts = 1_000_000;
        long topFollowers = 10_000_000L;
        long[] followers = new long[accounts];
        long total = 0;
        for (int rank = 1; rank <= accounts; rank++) {
            followers[rank - 1] = topFollowers / rank;
            total += followers[rank - 1];
        }
        System.out.printf(
                "  one post per account per day, push model: %,d timeline writes/day%n", total);
        System.out.printf(
                "  that is %,.0f writes/s on average%n", (double) total / SECONDS_PER_DAY);

        for (long threshold : new long[] {100_000L, 10_000L, 1_000L}) {
            long pushed = 0;
            int celebrities = 0;
            for (long f : followers) {
                if (f > threshold) {
                    celebrities++;
                } else {
                    pushed += f;
                }
            }
            double saved = 1.0 - (double) pushed / total;
            System.out.printf(
                    "  pull for accounts over %,7d followers: %,6d accounts (%.3f%%)"
                            + " remove %.0f%% of fan-out writes%n",
                    threshold, celebrities, 100.0 * celebrities / accounts, saved * 100);
            if (threshold == 10_000L) {
                check(celebrities == 999, "999 accounts exceed 10,000 followers");
                check(saved > 0.5, "0.1% of accounts cause over half the writes");
            }
        }
        long worst = followers[0];
        double seconds = worst / 50_000.0;
        System.out.printf(
                "  top account's single post at 50,000 writes/s: %,.0f s to fan out%n", seconds);
    }

    // ------------------------------------------------------------------
    // 4. Chat ordering and presence
    // ------------------------------------------------------------------

    /** Per-conversation receiver: delivers in sequence order, buffers early arrivals. */
    static final class ConversationReceiver {
        private long nextExpected = 1;
        private final TreeMap<Long, String> buffered = new TreeMap<>();
        private final List<String> delivered = new ArrayList<>();
        private final Set<Long> gapsRequested = new HashSet<>();

        void receive(long sequence, String body) {
            if (sequence < nextExpected || buffered.containsKey(sequence)) {
                return;
            }
            buffered.put(sequence, body);
            while (buffered.containsKey(nextExpected)) {
                delivered.add(buffered.remove(nextExpected));
                nextExpected++;
            }
            if (!buffered.isEmpty()) {
                for (long s = nextExpected; s < buffered.firstKey(); s++) {
                    gapsRequested.add(s);
                }
            }
        }
    }

    static void chat() {
        System.out.println();
        System.out.println("== 4. Chat: ordering and presence ==");
        ConversationReceiver receiver = new ConversationReceiver();
        long[] arrivals = {1, 2, 4, 5, 2, 3, 6};
        for (long s : arrivals) {
            receiver.receive(s, "m" + s);
        }
        System.out.printf(
                "  arrivals %s -> delivered %s, gap requested for %s%n",
                java.util.Arrays.toString(arrivals), receiver.delivered, receiver.gapsRequested);
        check(
                receiver.delivered.equals(List.of("m1", "m2", "m3", "m4", "m5", "m6")),
                "in-order, exactly-once delivery despite a gap and a duplicate");
        check(receiver.gapsRequested.equals(Set.of(3L)), "the missing sequence is requested");

        long online = 10_000_000L;
        int heartbeatSeconds = 30;
        int missedBeforeOffline = 3;
        System.out.printf(
                "  %,d online users, heartbeat every %ds: %,d heartbeats/s%n",
                online, heartbeatSeconds, online / heartbeatSeconds);
        System.out.printf(
                "  offline after %d missed beats: detected %d-%d s after disconnect%n",
                missedBeforeOffline,
                (missedBeforeOffline - 1) * heartbeatSeconds,
                missedBeforeOffline * heartbeatSeconds);
        long connectionsPerServer = 500_000L;
        System.out.printf(
                "  at %,d connections per gateway: %d gateways before headroom%n",
                connectionsPerServer, (online + connectionsPerServer - 1) / connectionsPerServer);
    }

    // ------------------------------------------------------------------
    // 5. Bloom filter
    // ------------------------------------------------------------------

    static final class BloomFilter {
        private final BitSet bits;
        private final int m;
        private final int k;

        BloomFilter(int m, int k) {
            this.bits = new BitSet(m);
            this.m = m;
            this.k = k;
        }

        private int index(long h1, long h2, int i) {
            return (int) Long.remainderUnsigned(h1 + i * h2, m);
        }

        void add(String key) {
            long h1 = hash(key);
            long h2 = mix64(h1) | 1;
            for (int i = 0; i < k; i++) {
                bits.set(index(h1, h2, i));
            }
        }

        boolean mightContain(String key) {
            long h1 = hash(key);
            long h2 = mix64(h1) | 1;
            for (int i = 0; i < k; i++) {
                if (!bits.get(index(h1, h2, i))) {
                    return false;
                }
            }
            return true;
        }
    }

    static void bloom() {
        System.out.println();
        System.out.println("== 5. Bloom filter: 100,000 members, 1% target ==");
        int n = 100_000;
        double target = 0.01;
        int m = (int) Math.ceil(-n * Math.log(target) / (Math.log(2) * Math.log(2)));
        int k = (int) Math.round((double) m / n * Math.log(2));
        double theoretical = Math.pow(1 - Math.exp(-(double) k * n / m), k);
        System.out.printf(
                "  m = %,d bits (%.0f KB), k = %d hashes, predicted FP %.3f%%%n",
                m, m / 8.0 / 1024, k, theoretical * 100);
        BloomFilter filter = new BloomFilter(m, k);
        for (int i = 0; i < n; i++) {
            filter.add("user-" + i);
        }
        for (int i = 0; i < n; i++) {
            check(filter.mightContain("user-" + i), "no false negatives");
        }
        int falsePositives = 0;
        int probes = 200_000;
        for (int i = 0; i < probes; i++) {
            if (filter.mightContain("other-" + i)) {
                falsePositives++;
            }
        }
        double measured = (double) falsePositives / probes;
        System.out.printf(
                "  measured on %,d non-members: %.3f%%; 0 false negatives on %,d members%n",
                probes, measured * 100, n);
        check(Math.abs(measured - theoretical) < 0.002, "measurement matches the formula");
    }

    // ------------------------------------------------------------------
    // 6. Geohash
    // ------------------------------------------------------------------

    static final String GEO32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    static String geohash(double lat, double lon, int precision) {
        double latLo = -90;
        double latHi = 90;
        double lonLo = -180;
        double lonHi = 180;
        StringBuilder out = new StringBuilder();
        boolean even = true;
        int bit = 0;
        int ch = 0;
        while (out.length() < precision) {
            if (even) {
                double mid = (lonLo + lonHi) / 2;
                if (lon >= mid) {
                    ch = (ch << 1) | 1;
                    lonLo = mid;
                } else {
                    ch <<= 1;
                    lonHi = mid;
                }
            } else {
                double mid = (latLo + latHi) / 2;
                if (lat >= mid) {
                    ch = (ch << 1) | 1;
                    latLo = mid;
                } else {
                    ch <<= 1;
                    latHi = mid;
                }
            }
            even = !even;
            if (++bit == 5) {
                out.append(GEO32.charAt(ch));
                bit = 0;
                ch = 0;
            }
        }
        return out.toString();
    }

    /** Longitude bounds of the cell containing (lat, lon) at the given precision. */
    static double[] lonBounds(double lon, int precision) {
        int lonBits = (5 * precision + 1) / 2;
        double width = 360.0 / (1L << lonBits);
        double lo = Math.floor((lon + 180) / width) * width - 180;
        return new double[] {lo, lo + width};
    }

    static void geohashes() {
        System.out.println();
        System.out.println("== 6. Geohash ==");
        double kmPerDegree = 111.32;
        System.out.println("  precision   cell at the equator (width x height)");
        for (int p = 4; p <= 8; p++) {
            int bits = 5 * p;
            int lonBits = (bits + 1) / 2;
            int latBits = bits / 2;
            double widthKm = 360.0 / (1L << lonBits) * kmPerDegree;
            double heightKm = 180.0 / (1L << latBits) * kmPerDegree;
            System.out.printf("  %d           %s x %s%n", p, distance(widthKm), distance(heightKm));
        }

        double lat = 37.7749;
        double lon = -122.4194;
        String sf = geohash(lat, lon, 8);
        System.out.printf("  San Francisco (37.7749, -122.4194) -> %s%n", sf);
        check(sf.startsWith("9q8yy"), "known geohash prefix for San Francisco");

        double edge = lonBounds(lon, 6)[1];
        double west = edge - 0.00003;
        double east = edge + 0.00003;
        double metres = (east - west) * kmPerDegree * 1000 * Math.cos(Math.toRadians(lat));
        String a = geohash(lat, west, 6);
        String b = geohash(lat, east, 6);
        int common = 0;
        while (common < 6 && a.charAt(common) == b.charAt(common)) {
            common++;
        }
        System.out.printf(
                "  two points %.1f m apart across a cell edge: %s vs %s (%d shared chars)%n",
                metres, a, b, common);
        check(!a.equals(b), "nearby points can land in different cells");
    }

    static String distance(double km) {
        return km >= 1 ? String.format("%.1f km", km) : String.format("%.0f m", km * 1000);
    }

    // ------------------------------------------------------------------
    // 7. Payments and booking
    // ------------------------------------------------------------------

    record Entry(String account, long cents) {}

    static final class PaymentService {
        private final Map<String, String> idempotency = new ConcurrentHashMap<>();
        private final List<List<Entry>> journal = new ArrayList<>();
        private final AtomicInteger charges = new AtomicInteger();

        synchronized String charge(String idempotencyKey, String customer, long cents) {
            String previous = idempotency.get(idempotencyKey);
            if (previous != null) {
                return previous;
            }
            String paymentId = "pay-" + (journal.size() + 1);
            journal.add(
                    List.of(
                            new Entry("customer:" + customer, -cents),
                            new Entry("merchant:revenue", cents)));
            charges.incrementAndGet();
            idempotency.put(idempotencyKey, paymentId);
            return paymentId;
        }

        long balance(String account) {
            long total = 0;
            for (List<Entry> txn : journal) {
                for (Entry e : txn) {
                    if (e.account().equals(account)) {
                        total += e.cents();
                    }
                }
            }
            return total;
        }

        boolean everyTransactionBalances() {
            for (List<Entry> txn : journal) {
                if (txn.stream().mapToLong(Entry::cents).sum() != 0) {
                    return false;
                }
            }
            return true;
        }
    }

    static void payments() throws InterruptedException {
        System.out.println();
        System.out.println("== 7. Payments and booking ==");
        PaymentService service = new PaymentService();
        SplittableRandom random = new SplittableRandom(5);
        int orders = 100;
        int attempts = 0;
        for (int order = 0; order < orders; order++) {
            int deliveries = 1 + random.nextInt(4);
            for (int d = 0; d < deliveries; d++) {
                service.charge("order-" + order, "c" + (order % 7), 1_999);
                attempts++;
            }
        }
        System.out.printf(
                "  %d orders, %d charge requests (retries included) -> %d charges%n",
                orders, attempts, service.charges.get());
        System.out.printf(
                "  merchant revenue %s, every transaction sums to zero: %s%n",
                cents(service.balance("merchant:revenue")), service.everyTransactionBalances());
        check(service.charges.get() == orders, "one charge per order");
        check(service.balance("merchant:revenue") == orders * 1_999L, "revenue");
        check(service.everyTransactionBalances(), "double entry");

        int seats = 10;
        int buyers = 50;
        AtomicReferenceArray<String> seatHolder = new AtomicReferenceArray<>(seats);
        AtomicInteger booked = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int b = 0; b < buyers; b++) {
            String buyer = "buyer-" + b;
            int wanted = b % seats;
            Thread t =
                    new Thread(
                            () -> {
                                if (seatHolder.compareAndSet(wanted, null, buyer)) {
                                    booked.incrementAndGet();
                                }
                            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        Set<String> holders = new HashSet<>();
        for (int s = 0; s < seats; s++) {
            holders.add(seatHolder.get(s));
        }
        System.out.printf(
                "  %d buyers race for %d seats with compare-and-set: %d booked,"
                        + " %d distinct holders%n",
                buyers, seats, booked.get(), holders.size());
        check(booked.get() == seats && holders.size() == seats, "no seat sold twice");
    }

    static String cents(long c) {
        return String.format("$%,d.%02d", c / 100, c % 100);
    }

    // ------------------------------------------------------------------
    // 8. Storage durability and time-series volume
    // ------------------------------------------------------------------

    static double binomialTail(int n, int atLeast, double p) {
        double total = 0;
        for (int i = atLeast; i <= n; i++) {
            total += choose(n, i) * Math.pow(p, i) * Math.pow(1 - p, n - i);
        }
        return total;
    }

    static double choose(int n, int k) {
        double c = 1;
        for (int i = 1; i <= k; i++) {
            c = c * (n - k + i) / i;
        }
        return c;
    }

    static void storage() {
        System.out.println();
        System.out.println("== 8. Storage durability and time-series volume ==");
        double p = 0.01;
        System.out.printf("  chance a node fails before repair completes: %.0f%%%n", p * 100);
        double replication = binomialTail(3, 3, p);
        double rs63 = binomialTail(9, 4, p);
        double rs104 = binomialTail(14, 5, p);
        System.out.printf(
                "  3x replication      overhead 3.00x  P(loss per object) %.2e%n", replication);
        System.out.printf("  RS(6,3) erasure     overhead 1.50x  P(loss per object) %.2e%n", rs63);
        System.out.printf("  RS(10,4) erasure    overhead 1.40x  P(loss per object) %.2e%n", rs104);
        check(rs104 < replication, "RS(10,4) is more durable than 3x at under half the storage");

        long series = 1_000_000L;
        int intervalSeconds = 10;
        long pointsPerDay = series * (SECONDS_PER_DAY / intervalSeconds);
        double rawGb = pointsPerDay * 16 / 1e9;
        double compressedGb = pointsPerDay * 1.37 / 1e9;
        System.out.printf(
                "  %,d series every %ds: %,d points/day%n", series, intervalSeconds, pointsPerDay);
        System.out.printf(
                "  16 bytes/point raw: %.0f GB/day; ~1.37 bytes/point compressed:"
                        + " %.1f GB/day%n",
                rawGb, compressedGb);
        double rawYear = rawGb * 365 / 1000;
        double compressedYear = compressedGb * 365 / 1000;
        double tiered =
                compressedGb * 14 / 1000
                        + series * (1440L * 90) * 1.37 / 1e12
                        + series * (24L * 365) * 1.37 / 1e12;
        System.out.printf(
                "  one year at 10s: %.1f TB raw, %.2f TB compressed%n", rawYear, compressedYear);
        System.out.printf(
                "  compressed and tiered (10s for 14d, 1m for 90d, 1h for 1y): %.2f TB%n", tiered);
        System.out.printf(
                "  compression saves %.1fx; tiering saves a further %.1fx%n",
                rawYear / compressedYear, compressedYear / tiered);
        check(compressedYear / tiered > 10, "tiering matters as much as compression");
    }

    public static void main(String[] args) throws InterruptedException {
        urlShortener();
        consistentHashing();
        feedFanOut();
        chat();
        bloom();
        geohashes();
        payments();
        storage();
        System.out.println();
        System.out.println("All high-level design checks passed.");
    }
}
