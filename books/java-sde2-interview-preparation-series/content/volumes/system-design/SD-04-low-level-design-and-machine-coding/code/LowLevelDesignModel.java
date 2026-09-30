import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Executable model of the eight machine-coding problems in this volume.
 *
 * Every number quoted in the chapters is printed by this program, and every
 * invariant the chapters rely on is checked by it. There are no dependencies:
 * time is injected as a LongSupplier so every scenario is deterministic, and
 * randomized equivalence tests use fixed seeds. Run it with:
 *
 *     javac --release 21 LowLevelDesignModel.java && java -ea LowLevelDesignModel
 *
 * Sections map to chapters:
 *   1. Parking lot: compatibility, allocation policy, pricing, concurrency (ch 2)
 *   2. Vending machine: state machine, change-making, money conservation   (ch 3)
 *   3. Elevator: LOOK scheduling against first-come-first-served           (ch 3)
 *   4. LRU and LFU caches, TTL, checked against reference models           (ch 4)
 *   5. Rate limiters: fixed window, sliding log, token bucket              (ch 4)
 *   6. Split expenses: integer money, remainders, settlement               (ch 5)
 *   7. Job scheduler: dependencies, lifecycle, retries, cancellation       (ch 6)
 *   8. Notification service: channels, preferences, idempotency, fallback  (ch 6)
 */
public final class LowLevelDesignModel {

    private LowLevelDesignModel() {}

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("check failed: " + message);
        }
    }

    // ------------------------------------------------------------------
    // 1. Parking lot
    // ------------------------------------------------------------------

    enum VehicleType {
        MOTORCYCLE(SpotSize.SMALL),
        CAR(SpotSize.COMPACT),
        TRUCK(SpotSize.LARGE);

        final SpotSize minimumSpot;

        VehicleType(SpotSize minimumSpot) {
            this.minimumSpot = minimumSpot;
        }

        boolean fits(SpotSize size) {
            return size.ordinal() >= minimumSpot.ordinal();
        }
    }

    enum SpotSize {
        SMALL,
        COMPACT,
        LARGE
    }

    record Spot(String id, int level, SpotSize size) {}

    record Ticket(long ticketId, String plate, Spot spot, long entryMinute) {}

    /** The seam that lets allocation rules change without touching the lot. */
    interface AllocationPolicy {
        Optional<Spot> choose(List<Spot> freeSpots, VehicleType vehicle);
    }

    /** Takes the first compatible spot in declaration order. */
    static final class FirstFit implements AllocationPolicy {
        @Override
        public Optional<Spot> choose(List<Spot> freeSpots, VehicleType vehicle) {
            return freeSpots.stream().filter(s -> vehicle.fits(s.size())).findFirst();
        }
    }

    /** Takes the smallest compatible spot, preserving large spots for large vehicles. */
    static final class BestFit implements AllocationPolicy {
        @Override
        public Optional<Spot> choose(List<Spot> freeSpots, VehicleType vehicle) {
            return freeSpots.stream()
                    .filter(s -> vehicle.fits(s.size()))
                    .min(
                            Comparator.comparingInt((Spot s) -> s.size().ordinal())
                                    .thenComparingInt(Spot::level)
                                    .thenComparing(Spot::id));
        }
    }

    interface PricingPolicy {
        long priceCents(long minutesParked);
    }

    /** Free grace period, first hour, then per started hour, capped per day. */
    record HourlyPricing(
            long graceMinutes, long firstHourCents, long perHourCents, long dailyCapCents)
            implements PricingPolicy {
        @Override
        public long priceCents(long minutesParked) {
            if (minutesParked <= graceMinutes) {
                return 0;
            }
            long fullDays = minutesParked / (24 * 60);
            long remainder = minutesParked % (24 * 60);
            long partial = 0;
            if (remainder > 0) {
                long startedHours = (remainder + 59) / 60;
                partial =
                        Math.min(dailyCapCents, firstHourCents + (startedHours - 1) * perHourCents);
            }
            return fullDays * dailyCapCents + partial;
        }
    }

    /** One lock owns allocation: the check and the claim are one atomic step. */
    static final class ParkingLot {
        private final List<Spot> free;
        private final Map<Long, Ticket> active = new HashMap<>();
        private final AllocationPolicy allocation;
        private final PricingPolicy pricing;
        private final LongSupplier clockMinutes;
        private long nextTicket = 1;

        ParkingLot(
                List<Spot> spots,
                AllocationPolicy allocation,
                PricingPolicy pricing,
                LongSupplier clockMinutes) {
            this.free = new ArrayList<>(spots);
            this.allocation = allocation;
            this.pricing = pricing;
            this.clockMinutes = clockMinutes;
        }

        synchronized Optional<Ticket> park(String plate, VehicleType vehicle) {
            Optional<Spot> chosen = allocation.choose(free, vehicle);
            if (chosen.isEmpty()) {
                return Optional.empty();
            }
            Spot spot = chosen.get();
            free.remove(spot);
            Ticket ticket = new Ticket(nextTicket++, plate, spot, clockMinutes.getAsLong());
            active.put(ticket.ticketId(), ticket);
            return Optional.of(ticket);
        }

        synchronized long exit(long ticketId) {
            Ticket ticket = active.remove(ticketId);
            if (ticket == null) {
                throw new IllegalArgumentException("unknown or already-closed ticket " + ticketId);
            }
            free.add(ticket.spot());
            return pricing.priceCents(clockMinutes.getAsLong() - ticket.entryMinute());
        }

        synchronized int freeCount() {
            return free.size();
        }

        synchronized Set<String> occupiedSpotIds() {
            Set<String> ids = new HashSet<>();
            for (Ticket t : active.values()) {
                ids.add(t.spot().id());
            }
            return ids;
        }

        synchronized int activeCount() {
            return active.size();
        }
    }

    static List<Spot> spots(int count, SpotSize size) {
        List<Spot> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(new Spot(size.name().charAt(0) + "-" + i, i % 3, size));
        }
        return result;
    }

    static void parkingLot() throws InterruptedException {
        System.out.println("== 1. Parking lot ==");

        List<Spot> twoSpots =
                List.of(new Spot("L-1", 0, SpotSize.LARGE), new Spot("C-1", 0, SpotSize.COMPACT));
        for (AllocationPolicy policy : List.of(new FirstFit(), new BestFit())) {
            ParkingLot lot =
                    new ParkingLot(
                            twoSpots, policy, new HourlyPricing(15, 300, 200, 2_000), () -> 0);
            Optional<Ticket> car = lot.park("CAR-1", VehicleType.CAR);
            Optional<Ticket> truck = lot.park("TRUCK-1", VehicleType.TRUCK);
            System.out.printf(
                    "  %-8s car -> %s, truck -> %s%n",
                    policy.getClass().getSimpleName(),
                    car.map(t -> t.spot().id()).orElse("REJECTED"),
                    truck.map(t -> t.spot().id()).orElse("REJECTED"));
            if (policy instanceof FirstFit) {
                check(truck.isEmpty(), "first fit strands the truck");
            } else {
                check(truck.isPresent(), "best fit keeps the large spot for the truck");
            }
        }

        HourlyPricing pricing = new HourlyPricing(15, 300, 200, 2_000);
        long[] durations = {10, 15, 16, 60, 61, 300, 720, 1_440, 1_500};
        long[] expected = {0, 0, 300, 300, 500, 1_100, 2_000, 2_000, 2_300};
        for (int i = 0; i < durations.length; i++) {
            long price = pricing.priceCents(durations[i]);
            System.out.printf("  %,5d min -> %s%n", durations[i], dollars(price));
            check(price == expected[i], "pricing for " + durations[i] + " minutes");
        }

        long[] now = {0};
        ParkingLot clocked =
                new ParkingLot(spots(1, SpotSize.COMPACT), new BestFit(), pricing, () -> now[0]);
        Ticket ticket = clocked.park("KA-01", VehicleType.CAR).orElseThrow();
        now[0] = 61;
        check(clocked.exit(ticket.ticketId()) == 500, "exit charges 61 minutes");
        boolean doubleExitRejected = false;
        try {
            clocked.exit(ticket.ticketId());
        } catch (IllegalArgumentException expectedFailure) {
            doubleExitRejected = true;
        }
        check(doubleExitRejected, "a ticket can be closed only once");

        int spotCount = 500;
        int threads = 8;
        int attemptsPerThread = 1_000;
        ParkingLot contended =
                new ParkingLot(spots(spotCount, SpotSize.COMPACT), new BestFit(), pricing, () -> 0);
        AtomicInteger successes = new AtomicInteger();
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            int threadId = t;
            Thread worker =
                    new Thread(
                            () -> {
                                for (int i = 0; i < attemptsPerThread; i++) {
                                    if (contended
                                            .park("T" + threadId + "-" + i, VehicleType.CAR)
                                            .isPresent()) {
                                        successes.incrementAndGet();
                                    }
                                }
                            });
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
        System.out.printf(
                "  %d threads x %,d attempts on %d spots -> %d parked, %d distinct spots, %d"
                    + " free%n",
                threads,
                attemptsPerThread,
                spotCount,
                successes.get(),
                contended.occupiedSpotIds().size(),
                contended.freeCount());
        check(successes.get() == spotCount, "exactly one success per spot");
        check(contended.occupiedSpotIds().size() == spotCount, "no spot assigned twice");
        check(contended.activeCount() == spotCount && contended.freeCount() == 0, "counts agree");
    }

    // ------------------------------------------------------------------
    // 2. Vending machine
    // ------------------------------------------------------------------

    enum VendingState {
        IDLE,
        HAS_MONEY,
        OUT_OF_SERVICE
    }

    record Product(String code, String name, long priceCents) {}

    record Vend(
            boolean dispensed, String productCode, Map<Integer, Integer> change, String reason) {}

    /**
     * Bounded change-making: minimum coin count using only coins in inventory.
     * Returns empty when no combination exists. Dynamic programming over the
     * amount, expanding each coin into its available copies (0/1 knapsack).
     */
    static Optional<Map<Integer, Integer>> makeChange(
            long amountCents, Map<Integer, Integer> inventory) {
        int amount = Math.toIntExact(amountCents);
        int[] best = new int[amount + 1];
        java.util.Arrays.fill(best, Integer.MAX_VALUE);
        best[0] = 0;
        List<Integer> copies = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry :
                new TreeMap<>(inventory).descendingMap().entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                copies.add(entry.getKey());
            }
        }
        int[][] choice = new int[copies.size()][];
        for (int c = 0; c < copies.size(); c++) {
            int coin = copies.get(c);
            choice[c] = new int[amount + 1];
            for (int value = amount; value >= coin; value--) {
                if (best[value - coin] != Integer.MAX_VALUE
                        && best[value - coin] + 1 < best[value]) {
                    best[value] = best[value - coin] + 1;
                    choice[c][value] = 1;
                }
            }
        }
        if (best[amount] == Integer.MAX_VALUE) {
            return Optional.empty();
        }
        Map<Integer, Integer> result = new TreeMap<>(Comparator.reverseOrder());
        int value = amount;
        for (int c = copies.size() - 1; c >= 0 && value > 0; c--) {
            if (choice[c][value] == 1 && best[value - copies.get(c)] == best[value] - 1) {
                result.merge(copies.get(c), 1, Integer::sum);
                value -= copies.get(c);
            }
        }
        check(value == 0, "change reconstruction reaches zero");
        return Optional.of(result);
    }

    /** Greedy change with bounded inventory: the version most candidates write first. */
    static Optional<Map<Integer, Integer>> greedyChange(
            long amountCents, Map<Integer, Integer> inventory) {
        long remaining = amountCents;
        Map<Integer, Integer> result = new TreeMap<>(Comparator.reverseOrder());
        for (Map.Entry<Integer, Integer> entry :
                new TreeMap<>(inventory).descendingMap().entrySet()) {
            int coin = entry.getKey();
            int use = (int) Math.min(entry.getValue(), remaining / coin);
            if (use > 0) {
                result.put(coin, use);
                remaining -= (long) use * coin;
            }
        }
        return remaining == 0 ? Optional.of(result) : Optional.empty();
    }

    static long value(Map<Integer, Integer> coins) {
        long total = 0;
        for (Map.Entry<Integer, Integer> e : coins.entrySet()) {
            total += (long) e.getKey() * e.getValue();
        }
        return total;
    }

    static final class VendingMachine {
        private VendingState state = VendingState.IDLE;
        private final Map<String, Product> products = new HashMap<>();
        private final Map<String, Integer> stock = new HashMap<>();
        private final Map<Integer, Integer> coins = new TreeMap<>();
        private final Set<Integer> accepted = Set.of(5, 10, 25, 100);
        private long credit;
        private long takenCents;
        private long insertedCents;
        private long returnedCents;

        void addProduct(Product product, int count) {
            products.put(product.code(), product);
            stock.merge(product.code(), count, Integer::sum);
        }

        void loadCoins(int coin, int count) {
            coins.merge(coin, count, Integer::sum);
        }

        VendingState state() {
            return state;
        }

        void insert(int coin) {
            if (state == VendingState.OUT_OF_SERVICE) {
                throw new IllegalStateException("machine is out of service");
            }
            if (!accepted.contains(coin)) {
                throw new IllegalArgumentException("coin not accepted: " + coin);
            }
            coins.merge(coin, 1, Integer::sum);
            credit += coin;
            insertedCents += coin;
            state = VendingState.HAS_MONEY;
        }

        /** Every refusal happens before anything leaves the machine. */
        Vend select(String code) {
            if (state != VendingState.HAS_MONEY) {
                return new Vend(false, code, Map.of(), "insert money first");
            }
            Product product = products.get(code);
            if (product == null || stock.getOrDefault(code, 0) == 0) {
                return new Vend(false, code, Map.of(), "sold out");
            }
            if (credit < product.priceCents()) {
                return new Vend(false, code, Map.of(), "insufficient credit");
            }
            Optional<Map<Integer, Integer>> change =
                    makeChange(credit - product.priceCents(), coins);
            if (change.isEmpty()) {
                return new Vend(false, code, Map.of(), "exact change only");
            }
            change.get().forEach((coin, n) -> coins.merge(coin, -n, Integer::sum));
            stock.merge(code, -1, Integer::sum);
            takenCents += product.priceCents();
            returnedCents += value(change.get());
            credit = 0;
            state = VendingState.IDLE;
            return new Vend(true, code, change.get(), "ok");
        }

        Map<Integer, Integer> cancel() {
            if (state != VendingState.HAS_MONEY) {
                return Map.of();
            }
            Map<Integer, Integer> refund =
                    makeChange(credit, coins)
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "inserted coins must always be refundable"));
            refund.forEach((coin, n) -> coins.merge(coin, -n, Integer::sum));
            returnedCents += value(refund);
            credit = 0;
            state = VendingState.IDLE;
            return refund;
        }

        boolean moneyConserved() {
            return insertedCents == takenCents + returnedCents + credit;
        }
    }

    static void vendingMachine() {
        System.out.println();
        System.out.println("== 2. Vending machine ==");

        Map<Integer, Integer> inventory = Map.of(25, 1, 10, 3);
        Optional<Map<Integer, Integer>> greedy = greedyChange(30, inventory);
        Optional<Map<Integer, Integer>> bounded = makeChange(30, inventory);
        System.out.printf(
                "  change for 30c from {25x1, 10x3}: greedy %s, bounded DP %s%n",
                greedy.map(Object::toString).orElse("FAILS"),
                bounded.map(Object::toString).orElse("FAILS"));
        check(greedy.isEmpty(), "greedy takes the quarter and strands 5c");
        check(bounded.isPresent() && bounded.get().equals(Map.of(10, 3)), "three dimes");

        Random random = new Random(42);
        int trials = 5_000;
        int greedyMisses = 0;
        for (int trial = 0; trial < trials; trial++) {
            Map<Integer, Integer> inv = new HashMap<>();
            for (int coin : new int[] {5, 10, 25, 100}) {
                inv.put(coin, random.nextInt(4));
            }
            long amount = 5L * (1 + random.nextInt(40));
            Optional<Map<Integer, Integer>> dp = makeChange(amount, inv);
            Optional<Map<Integer, Integer>> gr = greedyChange(amount, inv);
            if (dp.isPresent()) {
                check(value(dp.get()) == amount, "dp change sums to the amount");
                for (Map.Entry<Integer, Integer> e : dp.get().entrySet()) {
                    check(e.getValue() <= inv.getOrDefault(e.getKey(), 0), "dp respects inventory");
                }
            }
            if (gr.isPresent()) {
                check(dp.isPresent(), "dp finds change whenever greedy does");
            }
            if (dp.isPresent() && gr.isEmpty()) {
                greedyMisses++;
            }
        }
        System.out.printf(
                "  random inventories: greedy failed where change existed in %d of %,d trials%n",
                greedyMisses, trials);
        check(greedyMisses > 0, "the greedy failure is not a contrived corner");

        VendingMachine machine = new VendingMachine();
        machine.addProduct(new Product("A1", "Water", 65), 2);
        machine.loadCoins(25, 1);
        machine.loadCoins(10, 3);
        machine.insert(100);
        Vend refused = machine.select("A1");
        System.out.printf(
                "  $1.00 for a 65c item with {25x1, 10x3} loaded -> %s%n",
                refused.dispensed()
                        ? "dispensed " + refused.change()
                        : "refused: " + refused.reason());
        check(refused.dispensed(), "35c = 25 + 10 is available");
        check(refused.change().equals(Map.of(25, 1, 10, 1)), "change is 25 + 10");

        machine.insert(100);
        Vend second = machine.select("A1");
        System.out.printf(
                "  second $1.00 for the same item -> %s%n",
                second.dispensed()
                        ? "dispensed " + second.change()
                        : "refused: " + second.reason());
        check(
                !second.dispensed() && second.reason().equals("exact change only"),
                "only 10+10 left, cannot make 35c; must refuse before dispensing");
        Map<Integer, Integer> refund = machine.cancel();
        System.out.printf("  cancel refunds %s, state %s%n", refund, machine.state());
        check(refund.equals(Map.of(100, 1)), "the inserted dollar comes back");
        check(machine.state() == VendingState.IDLE, "cancel returns to idle");
        check(machine.moneyConserved(), "inserted = taken + returned + credit");
    }

    // ------------------------------------------------------------------
    // 3. Elevator scheduling
    // ------------------------------------------------------------------

    enum Direction {
        UP,
        DOWN,
        IDLE
    }

    /** LOOK: keep moving while stops remain ahead, then reverse. */
    static final class LookElevator {
        private final TreeSet<Integer> stops = new TreeSet<>();
        private int floor;
        private Direction direction;

        LookElevator(int startFloor, Direction initial) {
            this.floor = startFloor;
            this.direction = initial;
        }

        void request(int target) {
            if (target != floor) {
                stops.add(target);
            }
        }

        /** Serves every pending stop and returns the visit order. */
        List<Integer> run(int[] travelled) {
            List<Integer> visited = new ArrayList<>();
            while (!stops.isEmpty()) {
                Integer next =
                        direction == Direction.DOWN ? stops.floor(floor) : stops.ceiling(floor);
                if (next == null) {
                    direction = direction == Direction.DOWN ? Direction.UP : Direction.DOWN;
                    continue;
                }
                travelled[0] += Math.abs(next - floor);
                floor = next;
                stops.remove(next);
                visited.add(next);
            }
            direction = Direction.IDLE;
            return visited;
        }
    }

    static int fcfsDistance(int start, int[] requests) {
        int distance = 0;
        int floor = start;
        for (int target : requests) {
            distance += Math.abs(target - floor);
            floor = target;
        }
        return distance;
    }

    static void elevator() {
        System.out.println();
        System.out.println("== 3. Elevator: LOOK against first-come-first-served ==");
        int start = 5;
        int[] requests = {1, 9, 2, 8, 3};
        int fcfs = fcfsDistance(start, requests);
        System.out.printf("  start %d, requests %s%n", start, java.util.Arrays.toString(requests));
        System.out.printf("  FCFS travels %d floors%n", fcfs);
        check(fcfs == 30, "FCFS distance");
        for (Direction initial : List.of(Direction.UP, Direction.DOWN)) {
            LookElevator car = new LookElevator(start, initial);
            for (int r : requests) {
                car.request(r);
            }
            int[] travelled = {0};
            List<Integer> order = car.run(travelled);
            System.out.printf(
                    "  LOOK starting %-4s visits %s, travels %d floors%n",
                    initial, order, travelled[0]);
            check(travelled[0] == 12, "LOOK distance");
        }

        int[] counter = {4, 20};
        int counterFcfs = fcfsDistance(start, counter);
        int counterLookUp = lookDistance(start, Direction.UP, counter);
        int counterLookDown = lookDistance(start, Direction.DOWN, counter);
        System.out.printf(
                "  start %d, requests %s: FCFS %d, LOOK up-first %d, LOOK down-first %d%n",
                start,
                java.util.Arrays.toString(counter),
                counterFcfs,
                counterLookUp,
                counterLookDown);
        check(counterLookUp > counterFcfs, "a fixed initial direction can lose to FCFS");

        Random random = new Random(7);
        int trials = 2_000;
        int fixedUpWorse = 0;
        int bestDirectionWorse = 0;
        int bestDirectionNotOptimal = 0;
        long fcfsTotal = 0;
        long lookTotal = 0;
        for (int trial = 0; trial < trials; trial++) {
            int s = random.nextInt(20);
            int[] batch = new int[8];
            for (int i = 0; i < batch.length; i++) {
                batch[i] = random.nextInt(20);
            }
            int f = fcfsDistance(s, batch);
            int up = lookDistance(s, Direction.UP, batch);
            int best = Math.min(up, lookDistance(s, Direction.DOWN, batch));
            fcfsTotal += f;
            lookTotal += best;
            if (up > f) {
                fixedUpWorse++;
            }
            if (best > f) {
                bestDirectionWorse++;
            }
            if (best != optimalSweep(s, batch)) {
                bestDirectionNotOptimal++;
            }
        }
        System.out.printf("  %,d random batches of 8 on 20 floors:%n", trials);
        System.out.printf("    LOOK always starting UP loses to FCFS in %d%n", fixedUpWorse);
        System.out.printf(
                "    LOOK in the better direction loses in %d, is optimal in %,d%n",
                bestDirectionWorse, trials - bestDirectionNotOptimal);
        System.out.printf(
                "    mean travel %.1f floors (best-direction LOOK) vs %.1f (FCFS)%n",
                (double) lookTotal / trials, (double) fcfsTotal / trials);
        check(fixedUpWorse > 0, "the fixed-direction loss is not a one-off");
        check(
                bestDirectionWorse == 0 && bestDirectionNotOptimal == 0,
                "for a batch known up front, the better of the two sweeps is optimal");
    }

    static int lookDistance(int start, Direction initial, int[] requests) {
        LookElevator car = new LookElevator(start, initial);
        for (int r : requests) {
            car.request(r);
        }
        int[] travelled = {0};
        car.run(travelled);
        return travelled[0];
    }

    /** Optimal static tour on a line: reach one extreme, then sweep to the other. */
    static int optimalSweep(int start, int[] requests) {
        int low = start;
        int high = start;
        for (int r : requests) {
            low = Math.min(low, r);
            high = Math.max(high, r);
        }
        return Math.min((start - low) + (high - low), (high - start) + (high - low));
    }

    // ------------------------------------------------------------------
    // 4. Caches
    // ------------------------------------------------------------------

    interface Cache<K, V> {
        Optional<V> get(K key);

        void put(K key, V value);

        int size();
    }

    /** LRU on LinkedHashMap: access order plus removeEldestEntry. */
    static final class LinkedHashMapLru<K, V> implements Cache<K, V> {
        private final LinkedHashMap<K, V> map;

        LinkedHashMapLru(int capacity) {
            this.map =
                    new LinkedHashMap<>(16, 0.75f, true) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                            return size() > capacity;
                        }
                    };
        }

        @Override
        public synchronized Optional<V> get(K key) {
            return Optional.ofNullable(map.get(key));
        }

        @Override
        public synchronized void put(K key, V value) {
            map.put(key, value);
        }

        @Override
        public synchronized int size() {
            return map.size();
        }
    }

    /** LRU by hand: hash map to nodes of a doubly linked list with sentinels. */
    static final class HandRolledLru<K, V> implements Cache<K, V> {
        private static final class Node<K, V> {
            K key;
            V value;
            Node<K, V> prev;
            Node<K, V> next;
        }

        private final int capacity;
        private final Map<K, Node<K, V>> index = new HashMap<>();
        private final Node<K, V> head = new Node<>();
        private final Node<K, V> tail = new Node<>();

        HandRolledLru(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be positive");
            }
            this.capacity = capacity;
            head.next = tail;
            tail.prev = head;
        }

        private void unlink(Node<K, V> node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }

        private void linkFront(Node<K, V> node) {
            node.next = head.next;
            node.prev = head;
            head.next.prev = node;
            head.next = node;
        }

        @Override
        public Optional<V> get(K key) {
            Node<K, V> node = index.get(key);
            if (node == null) {
                return Optional.empty();
            }
            unlink(node);
            linkFront(node);
            return Optional.of(node.value);
        }

        @Override
        public void put(K key, V value) {
            Node<K, V> node = index.get(key);
            if (node != null) {
                node.value = value;
                unlink(node);
                linkFront(node);
                return;
            }
            if (index.size() == capacity) {
                Node<K, V> eldest = tail.prev;
                unlink(eldest);
                index.remove(eldest.key);
            }
            Node<K, V> fresh = new Node<>();
            fresh.key = key;
            fresh.value = value;
            linkFront(fresh);
            index.put(key, fresh);
        }

        @Override
        public int size() {
            return index.size();
        }
    }

    /**
     * O(1) LFU: key -> value, key -> frequency, frequency -> keys in recency
     * order, and the current minimum frequency. Ties evict the least recent.
     */
    static final class Lfu<K, V> implements Cache<K, V> {
        private final int capacity;
        private final Map<K, V> values = new HashMap<>();
        private final Map<K, Integer> counts = new HashMap<>();
        private final Map<Integer, LinkedHashSet<K>> buckets = new HashMap<>();
        private int minFrequency;

        Lfu(int capacity) {
            this.capacity = capacity;
        }

        private void touch(K key) {
            int count = counts.get(key);
            LinkedHashSet<K> bucket = buckets.get(count);
            bucket.remove(key);
            if (bucket.isEmpty()) {
                buckets.remove(count);
                if (minFrequency == count) {
                    minFrequency = count + 1;
                }
            }
            counts.put(key, count + 1);
            buckets.computeIfAbsent(count + 1, c -> new LinkedHashSet<>()).add(key);
        }

        @Override
        public Optional<V> get(K key) {
            if (!values.containsKey(key)) {
                return Optional.empty();
            }
            touch(key);
            return Optional.of(values.get(key));
        }

        @Override
        public void put(K key, V value) {
            if (capacity == 0) {
                return;
            }
            if (values.containsKey(key)) {
                values.put(key, value);
                touch(key);
                return;
            }
            if (values.size() == capacity) {
                LinkedHashSet<K> bucket = buckets.get(minFrequency);
                K victim = bucket.iterator().next();
                bucket.remove(victim);
                if (bucket.isEmpty()) {
                    buckets.remove(minFrequency);
                }
                values.remove(victim);
                counts.remove(victim);
            }
            values.put(key, value);
            counts.put(key, 1);
            buckets.computeIfAbsent(1, c -> new LinkedHashSet<>()).add(key);
            minFrequency = 1;
        }

        @Override
        public int size() {
            return values.size();
        }
    }

    /** Reference LFU: linear scans, obviously correct, used only to check the fast one. */
    static final class ReferenceLfu<K, V> implements Cache<K, V> {
        private final int capacity;
        private final Map<K, V> values = new HashMap<>();
        private final Map<K, Integer> counts = new HashMap<>();
        private final Map<K, Long> lastUse = new HashMap<>();
        private long tick;

        ReferenceLfu(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public Optional<V> get(K key) {
            if (!values.containsKey(key)) {
                return Optional.empty();
            }
            counts.merge(key, 1, Integer::sum);
            lastUse.put(key, tick++);
            return Optional.of(values.get(key));
        }

        @Override
        public void put(K key, V value) {
            if (values.containsKey(key)) {
                values.put(key, value);
                counts.merge(key, 1, Integer::sum);
                lastUse.put(key, tick++);
                return;
            }
            if (values.size() == capacity) {
                K victim = null;
                for (K candidate : values.keySet()) {
                    if (victim == null
                            || counts.get(candidate) < counts.get(victim)
                            || (counts.get(candidate).equals(counts.get(victim))
                                    && lastUse.get(candidate) < lastUse.get(victim))) {
                        victim = candidate;
                    }
                }
                values.remove(victim);
                counts.remove(victim);
                lastUse.remove(victim);
            }
            values.put(key, value);
            counts.put(key, 1);
            lastUse.put(key, tick++);
        }

        @Override
        public int size() {
            return values.size();
        }
    }

    /** Lazy expiry: an entry past its deadline is a miss and is removed on read. */
    static final class TtlCache<K, V> {
        private record Entry<V>(V value, long expiresAtMillis) {}

        private final Map<K, Entry<V>> map = new HashMap<>();
        private final LongSupplier clockMillis;

        TtlCache(LongSupplier clockMillis) {
            this.clockMillis = clockMillis;
        }

        void put(K key, V value, long ttlMillis) {
            map.put(key, new Entry<>(value, clockMillis.getAsLong() + ttlMillis));
        }

        Optional<V> get(K key) {
            Entry<V> entry = map.get(key);
            if (entry == null) {
                return Optional.empty();
            }
            if (clockMillis.getAsLong() >= entry.expiresAtMillis()) {
                map.remove(key);
                return Optional.empty();
            }
            return Optional.of(entry.value());
        }

        int storedEntries() {
            return map.size();
        }
    }

    static int compare(
            Cache<Integer, Integer> fast,
            Cache<Integer, Integer> reference,
            long seed,
            int operations,
            int keySpace) {
        Random random = new Random(seed);
        int mismatches = 0;
        for (int op = 0; op < operations; op++) {
            int key = random.nextInt(keySpace);
            if (random.nextBoolean()) {
                if (!fast.get(key).equals(reference.get(key))) {
                    mismatches++;
                }
            } else {
                fast.put(key, op);
                reference.put(key, op);
            }
            if (fast.size() != reference.size()) {
                mismatches++;
            }
        }
        return mismatches;
    }

    static void caches() {
        System.out.println();
        System.out.println("== 4. Caches ==");
        int lruMismatches =
                compare(new HandRolledLru<>(8), new LinkedHashMapLru<>(8), 11, 200_000, 20);
        System.out.printf(
                "  hand-rolled LRU vs LinkedHashMap LRU: %d mismatches over 200,000 ops%n",
                lruMismatches);
        check(lruMismatches == 0, "hand-rolled LRU agrees with LinkedHashMap");

        int lfuMismatches = compare(new Lfu<>(8), new ReferenceLfu<>(8), 13, 200_000, 20);
        System.out.printf(
                "  O(1) LFU vs linear-scan reference:     %d mismatches over 200,000 ops%n",
                lfuMismatches);
        check(lfuMismatches == 0, "O(1) LFU agrees with the reference");

        HandRolledLru<String, Integer> lru = new HandRolledLru<>(2);
        Lfu<String, Integer> lfu = new Lfu<>(2);
        for (Cache<String, Integer> cache : List.<Cache<String, Integer>>of(lru, lfu)) {
            cache.put("a", 1);
            cache.put("b", 2);
            cache.get("a");
            cache.get("a");
            cache.get("b");
            cache.put("c", 3);
        }
        System.out.printf(
                "  put a, put b, get a, get a, get b, put c:  LRU keeps a=%s b=%s | LFU keeps a=%s"
                    + " b=%s%n",
                lru.get("a").isPresent(),
                lru.get("b").isPresent(),
                lfu.get("a").isPresent(),
                lfu.get("b").isPresent());
        check(lru.get("a").isEmpty(), "LRU evicts a: b was touched more recently");
        check(
                lfu.get("a").isPresent() && lfu.get("b").isEmpty(),
                "LFU evicts b: a was used more often");

        long[] now = {0};
        TtlCache<String, String> ttl = new TtlCache<>(() -> now[0]);
        ttl.put("session", "alice", 1_000);
        now[0] = 999;
        check(ttl.get("session").isPresent(), "alive before the deadline");
        now[0] = 1_000;
        check(ttl.get("session").isEmpty(), "expired at the deadline");
        check(ttl.storedEntries() == 0, "expired entry removed on read");
        for (int i = 0; i < 1_000; i++) {
            ttl.put("k" + i, "v", 10);
        }
        now[0] = 5_000;
        System.out.printf(
                "  1,000 entries written with 10ms TTL, none read: %,d still stored after 4s%n",
                ttl.storedEntries());
        check(ttl.storedEntries() == 1_000, "lazy expiry alone never frees unread keys");
    }

    // ------------------------------------------------------------------
    // 5. Rate limiters
    // ------------------------------------------------------------------

    interface RateLimiter {
        boolean tryAcquire(long nowMillis);
    }

    static final class FixedWindow implements RateLimiter {
        private final int limit;
        private final long windowMillis;
        private long currentWindow = -1;
        private int count;

        FixedWindow(int limit, long windowMillis) {
            this.limit = limit;
            this.windowMillis = windowMillis;
        }

        @Override
        public boolean tryAcquire(long nowMillis) {
            long window = nowMillis / windowMillis;
            if (window != currentWindow) {
                currentWindow = window;
                count = 0;
            }
            if (count < limit) {
                count++;
                return true;
            }
            return false;
        }
    }

    static final class SlidingLog implements RateLimiter {
        private final int limit;
        private final long windowMillis;
        private final Deque<Long> log = new ArrayDeque<>();

        SlidingLog(int limit, long windowMillis) {
            this.limit = limit;
            this.windowMillis = windowMillis;
        }

        @Override
        public boolean tryAcquire(long nowMillis) {
            while (!log.isEmpty() && log.peekFirst() <= nowMillis - windowMillis) {
                log.pollFirst();
            }
            if (log.size() < limit) {
                log.addLast(nowMillis);
                return true;
            }
            return false;
        }
    }

    /** Integer token bucket: tokens held in thousandths so refill needs no floating point. */
    static final class TokenBucket implements RateLimiter {
        private final long capacityMilli;
        private final long refillMilliPerMs;
        private long tokensMilli;
        private long lastMillis;

        TokenBucket(int capacity, int tokensPerSecond, long startMillis) {
            this.capacityMilli = capacity * 1_000L;
            this.refillMilliPerMs = tokensPerSecond;
            this.tokensMilli = capacityMilli;
            this.lastMillis = startMillis;
        }

        @Override
        public boolean tryAcquire(long nowMillis) {
            long elapsed = Math.max(0, nowMillis - lastMillis);
            tokensMilli = Math.min(capacityMilli, tokensMilli + elapsed * refillMilliPerMs);
            lastMillis = Math.max(lastMillis, nowMillis);
            if (tokensMilli >= 1_000) {
                tokensMilli -= 1_000;
                return true;
            }
            return false;
        }
    }

    static int burst(RateLimiter limiter, long atMillis, int attempts) {
        int allowed = 0;
        for (int i = 0; i < attempts; i++) {
            if (limiter.tryAcquire(atMillis)) {
                allowed++;
            }
        }
        return allowed;
    }

    static void rateLimiters() {
        System.out.println();
        System.out.println("== 5. Rate limiters: 10 requests per 10 seconds ==");
        List<RateLimiter> limiters =
                List.of(
                        new FixedWindow(10, 10_000),
                        new SlidingLog(10, 10_000),
                        new TokenBucket(10, 1, 0));
        List<String> names = List.of("fixed window", "sliding log", "token bucket");
        int[] expected = {20, 10, 10};
        for (int i = 0; i < limiters.size(); i++) {
            RateLimiter limiter = limiters.get(i);
            int first = burst(limiter, 9_900, 15);
            int second = burst(limiter, 10_000, 15);
            System.out.printf(
                    "  %-13s 15 at t=9.9s -> %2d, 15 at t=10.0s -> %2d, total %2d in 100ms%n",
                    names.get(i), first, second, first + second);
            check(first + second == expected[i], names.get(i) + " boundary behaviour");
        }

        TokenBucket steady = new TokenBucket(10, 1, 0);
        burst(steady, 0, 10);
        int later = burst(steady, 3_500, 10);
        System.out.printf(
                "  token bucket drained at t=0, 10 attempts at t=3.5s -> %d allowed%n", later);
        check(later == 3, "3.5 seconds refills three whole tokens");
    }

    // ------------------------------------------------------------------
    // 6. Split expenses
    // ------------------------------------------------------------------

    /** Equal split in cents; the remainder goes one cent at a time in participant order. */
    static Map<String, Long> splitEqually(long totalCents, List<String> participants) {
        long base = totalCents / participants.size();
        long remainder = totalCents % participants.size();
        Map<String, Long> shares = new LinkedHashMap<>();
        for (int i = 0; i < participants.size(); i++) {
            shares.put(participants.get(i), base + (i < remainder ? 1 : 0));
        }
        return shares;
    }

    /** Percentage split in basis points, largest-remainder method so shares sum exactly. */
    static Map<String, Long> splitByBasisPoints(long totalCents, Map<String, Integer> basisPoints) {
        int totalBp = basisPoints.values().stream().mapToInt(Integer::intValue).sum();
        if (totalBp != 10_000) {
            throw new IllegalArgumentException(
                    "percentages must sum to 100.00%, got " + totalBp + " bp");
        }
        Map<String, Long> shares = new LinkedHashMap<>();
        Map<String, Long> remainders = new LinkedHashMap<>();
        long assigned = 0;
        for (Map.Entry<String, Integer> e : basisPoints.entrySet()) {
            long exact = totalCents * e.getValue();
            shares.put(e.getKey(), exact / 10_000);
            remainders.put(e.getKey(), exact % 10_000);
            assigned += exact / 10_000;
        }
        List<String> byRemainder = new ArrayList<>(remainders.keySet());
        byRemainder.sort(Comparator.comparingLong((String k) -> remainders.get(k)).reversed());
        for (int i = 0; i < totalCents - assigned; i++) {
            shares.merge(byRemainder.get(i), 1L, Long::sum);
        }
        return shares;
    }

    record Transfer(String from, String to, long cents) {}

    /** Largest creditor is paid by the largest debtor until every balance is zero. */
    static List<Transfer> settleGreedy(Map<String, Long> balances) {
        Map<String, Long> b = new TreeMap<>(balances);
        List<Transfer> transfers = new ArrayList<>();
        while (true) {
            String creditor = null;
            String debtor = null;
            for (Map.Entry<String, Long> e : b.entrySet()) {
                if (creditor == null || e.getValue() > b.get(creditor)) {
                    creditor = e.getKey();
                }
                if (debtor == null || e.getValue() < b.get(debtor)) {
                    debtor = e.getKey();
                }
            }
            if (creditor == null || b.get(creditor) == 0) {
                return transfers;
            }
            long amount = Math.min(b.get(creditor), -b.get(debtor));
            transfers.add(new Transfer(debtor, creditor, amount));
            b.merge(creditor, -amount, Long::sum);
            b.merge(debtor, amount, Long::sum);
        }
    }

    /**
     * Minimum number of transfers: n nonzero balances split into the largest
     * number of zero-sum groups g need exactly n - g transfers. Exponential,
     * which is the point: the exact answer is NP-hard, so it is fine for a
     * dinner bill and wrong for a production ledger.
     */
    static int minimumTransfers(Map<String, Long> balances) {
        long[] nz =
                balances.values().stream().mapToLong(Long::longValue).filter(v -> v != 0).toArray();
        int n = nz.length;
        long[] sum = new long[1 << n];
        int[] groups = new int[1 << n];
        for (int mask = 1; mask < (1 << n); mask++) {
            int low = Integer.numberOfTrailingZeros(mask);
            sum[mask] = sum[mask & (mask - 1)] + nz[low];
            int best = 0;
            for (int i = 0; i < n; i++) {
                if ((mask >> i & 1) == 1) {
                    best = Math.max(best, groups[mask ^ (1 << i)]);
                }
            }
            groups[mask] = best + (sum[mask] == 0 ? 1 : 0);
        }
        return n - groups[(1 << n) - 1];
    }

    static void splitExpenses() {
        System.out.println();
        System.out.println("== 6. Split expenses ==");

        double perPerson = Math.round(100.00 / 3 * 100) / 100.0;
        double rebuilt = perPerson * 3;
        System.out.printf(
                "  double: $100 / 3 rounded to cents = %.2f each, x3 = %.2f%n", perPerson, rebuilt);
        check(Math.abs(rebuilt - 100.0) > 0.001, "double rounding loses a cent");
        System.out.printf("  double: 0.1 + 0.2 == 0.3 is %s%n", 0.1 + 0.2 == 0.3);

        Map<String, Long> equal = splitEqually(10_000, List.of("asha", "ben", "chen"));
        System.out.printf(
                "  cents:  $100 / 3 -> %s, sum %s%n",
                equal, dollars(equal.values().stream().mapToLong(Long::longValue).sum()));
        check(
                equal.values().stream().mapToLong(Long::longValue).sum() == 10_000,
                "equal split preserves the total");

        Map<String, Integer> thirds = new LinkedHashMap<>();
        thirds.put("asha", 3_334);
        thirds.put("ben", 3_333);
        thirds.put("chen", 3_333);
        Map<String, Long> pct = splitByBasisPoints(10_001, thirds);
        System.out.printf("  $100.01 at 33.34/33.33/33.33%% -> %s%n", pct);
        check(
                pct.values().stream().mapToLong(Long::longValue).sum() == 10_001,
                "percentage split preserves the total");

        Random random = new Random(3);
        for (int trial = 0; trial < 10_000; trial++) {
            long total = 1 + random.nextInt(1_000_000);
            int a = random.nextInt(10_001);
            int b = random.nextInt(10_001 - a);
            Map<String, Integer> bp = new LinkedHashMap<>();
            bp.put("x", a);
            bp.put("y", b);
            bp.put("z", 10_000 - a - b);
            long sum =
                    splitByBasisPoints(total, bp).values().stream()
                            .mapToLong(Long::longValue)
                            .sum();
            check(sum == total, "largest remainder preserves every total");
        }
        System.out.println(
                "  largest-remainder split: 10,000 random totals and percentages, every sum exact");

        Map<String, Long> balances = new TreeMap<>();
        balances.put("A", 3_000L);
        balances.put("B", 2_000L);
        balances.put("C", 2_000L);
        balances.put("D", -4_000L);
        balances.put("E", -3_000L);
        check(
                balances.values().stream().mapToLong(Long::longValue).sum() == 0,
                "balances sum to zero");
        List<Transfer> greedy = settleGreedy(balances);
        int optimal = minimumTransfers(balances);
        System.out.printf("  balances %s%n", balances);
        System.out.printf("  greedy settles in %d transfers: %s%n", greedy.size(), greedy);
        System.out.printf("  minimum is %d: {A,E} and {B,C,D} are zero-sum groups%n", optimal);
        check(greedy.size() == 4 && optimal == 3, "greedy is not optimal here");

        Map<String, Long> after = new TreeMap<>(balances);
        for (Transfer t : greedy) {
            after.merge(t.from(), t.cents(), Long::sum);
            after.merge(t.to(), -t.cents(), Long::sum);
        }
        check(
                after.values().stream().allMatch(v -> v == 0),
                "greedy transfers clear every balance");
    }

    // ------------------------------------------------------------------
    // 7. Job scheduler
    // ------------------------------------------------------------------

    enum JobState {
        PENDING,
        READY,
        RUNNING,
        SUCCEEDED,
        FAILED,
        CANCELLED;

        private static final Map<JobState, Set<JobState>> ALLOWED = new EnumMap<>(JobState.class);

        static {
            ALLOWED.put(PENDING, EnumSet.of(READY, CANCELLED));
            ALLOWED.put(READY, EnumSet.of(RUNNING, CANCELLED));
            ALLOWED.put(RUNNING, EnumSet.of(SUCCEEDED, READY, FAILED));
            ALLOWED.put(SUCCEEDED, EnumSet.noneOf(JobState.class));
            ALLOWED.put(FAILED, EnumSet.noneOf(JobState.class));
            ALLOWED.put(CANCELLED, EnumSet.noneOf(JobState.class));
        }

        boolean canMoveTo(JobState next) {
            return ALLOWED.get(this).contains(next);
        }
    }

    record Job(String id, int priority, Set<String> dependsOn) {}

    /** Kahn's algorithm with a priority queue, so ties resolve deterministically. */
    static List<String> executionOrder(Map<String, Job> jobs) {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        for (Job job : jobs.values()) {
            indegree.putIfAbsent(job.id(), 0);
            for (String dependency : job.dependsOn()) {
                if (!jobs.containsKey(dependency)) {
                    throw new IllegalArgumentException(
                            job.id() + " depends on unknown job " + dependency);
                }
                indegree.merge(job.id(), 1, Integer::sum);
                dependents.computeIfAbsent(dependency, d -> new ArrayList<>()).add(job.id());
            }
        }
        PriorityQueue<Job> ready =
                new PriorityQueue<>(
                        Comparator.comparingInt(Job::priority).reversed().thenComparing(Job::id));
        indegree.forEach(
                (id, degree) -> {
                    if (degree == 0) {
                        ready.add(jobs.get(id));
                    }
                });
        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            Job job = ready.poll();
            order.add(job.id());
            for (String next : dependents.getOrDefault(job.id(), List.of())) {
                if (indegree.merge(next, -1, Integer::sum) == 0) {
                    ready.add(jobs.get(next));
                }
            }
        }
        if (order.size() != jobs.size()) {
            Set<String> stuck = new TreeSet<>(jobs.keySet());
            stuck.removeAll(order);
            throw new IllegalStateException("dependency cycle; stuck jobs " + stuck);
        }
        return order;
    }

    /** Everything that transitively depends on the cancelled job can never run. */
    static Set<String> cancellationClosure(Map<String, Job> jobs, String cancelled) {
        Map<String, List<String>> dependents = new HashMap<>();
        for (Job job : jobs.values()) {
            for (String dependency : job.dependsOn()) {
                dependents.computeIfAbsent(dependency, d -> new ArrayList<>()).add(job.id());
            }
        }
        Set<String> closure = new TreeSet<>();
        Deque<String> work = new ArrayDeque<>(List.of(cancelled));
        while (!work.isEmpty()) {
            String id = work.pop();
            if (closure.add(id)) {
                work.addAll(dependents.getOrDefault(id, List.of()));
            }
        }
        return closure;
    }

    static List<Long> backoffSchedule(long baseMillis, long capMillis, int attempts) {
        List<Long> delays = new ArrayList<>();
        long delay = baseMillis;
        for (int i = 0; i < attempts; i++) {
            delays.add(Math.min(delay, capMillis));
            delay = Math.min(delay * 2, capMillis);
        }
        return delays;
    }

    static final class TrackedJob {
        private JobState state = JobState.PENDING;
        private final List<JobState> history = new ArrayList<>(List.of(JobState.PENDING));

        void move(JobState next) {
            if (!state.canMoveTo(next)) {
                throw new IllegalStateException("illegal transition " + state + " -> " + next);
            }
            state = next;
            history.add(next);
        }
    }

    static void jobScheduler() {
        System.out.println();
        System.out.println("== 7. Job scheduler ==");
        Map<String, Job> jobs = new LinkedHashMap<>();
        jobs.put("fetch", new Job("fetch", 1, Set.of()));
        jobs.put("schema", new Job("schema", 5, Set.of()));
        jobs.put("clean", new Job("clean", 1, Set.of("fetch")));
        jobs.put("load", new Job("load", 1, Set.of("clean", "schema")));
        jobs.put("index", new Job("index", 1, Set.of("load")));
        jobs.put("report", new Job("report", 3, Set.of("load")));
        List<String> order = executionOrder(jobs);
        System.out.printf("  execution order %s%n", order);
        check(
                order.equals(List.of("schema", "fetch", "clean", "load", "report", "index")),
                "priority breaks ties among ready jobs only");

        Map<String, Job> cyclic = new LinkedHashMap<>(jobs);
        cyclic.put("fetch", new Job("fetch", 1, Set.of("index")));
        String cycleMessage = "";
        try {
            executionOrder(cyclic);
        } catch (IllegalStateException expected) {
            cycleMessage = expected.getMessage();
        }
        System.out.printf("  fetch now depends on index -> %s%n", cycleMessage);
        check(cycleMessage.startsWith("dependency cycle"), "cycle reported, not an infinite wait");

        Set<String> cancelled = cancellationClosure(jobs, "clean");
        System.out.printf("  cancelling clean also cancels %s%n", cancelled);
        check(
                cancelled.equals(new TreeSet<>(List.of("clean", "load", "index", "report"))),
                "closure");

        List<Long> delays = backoffSchedule(100, 5_000, 8);
        System.out.printf("  backoff base 100ms, cap 5s: %s%n", delays);
        check(
                delays.equals(List.of(100L, 200L, 400L, 800L, 1_600L, 3_200L, 5_000L, 5_000L)),
                "backoff");

        TrackedJob flaky = new TrackedJob();
        int maxAttempts = 3;
        int failuresBeforeSuccess = 2;
        flaky.move(JobState.READY);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            flaky.move(JobState.RUNNING);
            if (attempt > failuresBeforeSuccess) {
                flaky.move(JobState.SUCCEEDED);
                break;
            }
            flaky.move(attempt == maxAttempts ? JobState.FAILED : JobState.READY);
        }
        System.out.printf("  flaky job, 3 attempts: %s%n", flaky.history);
        check(flaky.state == JobState.SUCCEEDED, "third attempt succeeds");

        TrackedJob done = new TrackedJob();
        done.move(JobState.READY);
        done.move(JobState.RUNNING);
        done.move(JobState.SUCCEEDED);
        boolean rejected = false;
        try {
            done.move(JobState.CANCELLED);
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        check(rejected, "a succeeded job cannot be cancelled");
    }

    // ------------------------------------------------------------------
    // 8. Notification service
    // ------------------------------------------------------------------

    enum Channel {
        PUSH,
        EMAIL,
        SMS
    }

    record Notification(
            String eventId, String userId, String template, Map<String, String> params) {}

    record DeliveryResult(boolean delivered, Channel channel, int attempts, String detail) {}

    interface ChannelSender {
        Channel channel();

        boolean send(String userId, String body);
    }

    /** Fails its first N calls, so retry and fallback are exercised deterministically. */
    static final class FlakySender implements ChannelSender {
        private final Channel channel;
        private int failuresRemaining;
        private final List<String> delivered = new ArrayList<>();

        FlakySender(Channel channel, int failures) {
            this.channel = channel;
            this.failuresRemaining = failures;
        }

        @Override
        public Channel channel() {
            return channel;
        }

        @Override
        public boolean send(String userId, String body) {
            if (failuresRemaining > 0) {
                failuresRemaining--;
                return false;
            }
            delivered.add(userId + ": " + body);
            return true;
        }
    }

    static String render(String template, Map<String, String> params) {
        String out = template;
        for (Map.Entry<String, String> e : params.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        if (out.contains("{")) {
            throw new IllegalArgumentException("unfilled template variable in: " + out);
        }
        return out;
    }

    static final class NotificationService {
        private final Map<Channel, ChannelSender> senders = new EnumMap<>(Channel.class);
        private final Map<String, List<Channel>> preferences = new HashMap<>();
        private final Map<String, DeliveryResult> sent = new HashMap<>();
        private final int attemptsPerChannel;

        NotificationService(List<ChannelSender> senders, int attemptsPerChannel) {
            for (ChannelSender sender : senders) {
                this.senders.put(sender.channel(), sender);
            }
            this.attemptsPerChannel = attemptsPerChannel;
        }

        void prefer(String userId, List<Channel> channels) {
            preferences.put(userId, List.copyOf(channels));
        }

        /** Idempotent per (event, user): a redelivered event returns the first result. */
        DeliveryResult notify(Notification n) {
            String key = n.eventId() + "|" + n.userId();
            DeliveryResult previous = sent.get(key);
            if (previous != null && previous.delivered()) {
                return new DeliveryResult(true, previous.channel(), 0, "duplicate suppressed");
            }
            String body = render(n.template(), n.params());
            int attempts = 0;
            for (Channel channel : preferences.getOrDefault(n.userId(), List.of(Channel.EMAIL))) {
                ChannelSender sender = senders.get(channel);
                if (sender == null) {
                    continue;
                }
                for (int i = 0; i < attemptsPerChannel; i++) {
                    attempts++;
                    if (sender.send(n.userId(), body)) {
                        DeliveryResult result = new DeliveryResult(true, channel, attempts, "sent");
                        sent.put(key, result);
                        return result;
                    }
                }
            }
            DeliveryResult failed =
                    new DeliveryResult(false, null, attempts, "all channels failed");
            sent.put(key, failed);
            return failed;
        }
    }

    static void notificationService() {
        System.out.println();
        System.out.println("== 8. Notification service ==");
        FlakySender push = new FlakySender(Channel.PUSH, 5);
        FlakySender email = new FlakySender(Channel.EMAIL, 1);
        NotificationService service = new NotificationService(List.of(push, email), 2);
        service.prefer("u1", List.of(Channel.PUSH, Channel.EMAIL));
        Notification shipped =
                new Notification(
                        "order-42-shipped",
                        "u1",
                        "Order {order} has shipped",
                        Map.of("order", "42"));

        DeliveryResult first = service.notify(shipped);
        System.out.printf(
                "  push fails 5x, email fails 1x, 2 attempts per channel -> %s via %s after %d"
                    + " attempts%n",
                first.detail(), first.channel(), first.attempts());
        check(
                first.delivered() && first.channel() == Channel.EMAIL && first.attempts() == 4,
                "fallback to email");

        DeliveryResult again = service.notify(shipped);
        System.out.printf(
                "  same event delivered again -> %s, email outbox size %d%n",
                again.detail(), email.delivered.size());
        check(
                again.detail().equals("duplicate suppressed") && email.delivered.size() == 1,
                "idempotent");

        boolean unfilled = false;
        try {
            render("Hi {name}, order {order}", Map.of("order", "42"));
        } catch (IllegalArgumentException expected) {
            unfilled = true;
        }
        check(unfilled, "a missing template parameter fails before sending");
        check(Objects.equals(render("Order {order}", Map.of("order", "7")), "Order 7"), "render");
    }

    static String dollars(long cents) {
        return String.format("$%d.%02d", cents / 100, cents % 100);
    }

    public static void main(String[] args) throws InterruptedException {
        parkingLot();
        vendingMachine();
        elevator();
        caches();
        rateLimiters();
        splitExpenses();
        jobScheduler();
        notificationService();
        System.out.println();
        System.out.println("All low-level design checks passed.");
    }
}
