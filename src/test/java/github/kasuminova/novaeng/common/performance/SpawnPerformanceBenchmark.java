package github.kasuminova.novaeng.common.performance;

import com.sun.management.ThreadMXBean;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.ChunkPos;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class SpawnPerformanceBenchmark {

    private static final int FORKS = 3;
    private static final int WARMUP_ROUNDS = 6;
    private static final int MEASUREMENT_ROUNDS = 9;
    private static final long ROUND_NANOS = 75_000_000L;
    private static volatile long blackhole;

    private SpawnPerformanceBenchmark() {
    }

    static void main(final String[] args) throws Exception {
        final String mode = args.length > 1 ? args[1] : "all";
        if (!mode.equals("all") && !mode.equals("reuse") && !mode.equals("ticks") && !mode.equals("worldgen") && !mode.equals("worldgen2") && !mode.equals("worldgen3")) {
            throw new IllegalArgumentException("Unknown benchmark mode: " + mode);
        }
        if (args.length > 0 && args[0].startsWith("--fork=")) {
            runFork(Integer.parseInt(args[0].substring("--fork=".length())), mode);
            return;
        }
        final Path report = Path.of(args.length == 0 ? "build/reports/benchmarks/spawning.csv" : args[0]);
        Files.createDirectories(report.toAbsolutePath().getParent());
        final List<String> results = new ArrayList<>();
        results.add("fork,scenario,variant,round,operations,ns_per_op,cpu_ns_per_op,bytes_per_op");
        System.out.println("Java: " + System.getProperty("java.runtime.version") + "; " + System.getProperty("java.vm.name"));
        System.out.println("Forks: " + FORKS + "; warmup rounds: " + WARMUP_ROUNDS + "; measurement rounds: "
            + MEASUREMENT_ROUNDS + "; round target: " + ROUND_NANOS / 1_000_000 + " ms");
        System.out.println("Child JVM: -Xms1g -Xmx1g -XX:+UseZGC -XX:+UseCompactObjectHeaders");
        for (int fork = 0; fork < FORKS; fork++) {
            final Path launchArguments = Files.createTempFile("nova-benchmark-fork-", ".args");
            try {
                final String classPath = System.getProperty("java.class.path").replace("\\", "\\\\").replace("\"", "\\\"");
                Files.write(launchArguments, List.of("-Xms1g", "-Xmx1g", "-XX:+UseZGC", "-XX:+UseCompactObjectHeaders",
                    "-cp", "\"" + classPath + "\"", SpawnPerformanceBenchmark.class.getName(), "--fork=" + fork, mode), StandardCharsets.UTF_8);
                final Process child = new ProcessBuilder(ProcessHandle.current().info().command().orElseThrow(),
                    "@" + launchArguments.toAbsolutePath()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(child.getInputStream(), StandardCharsets.UTF_8))) {
                    for (String line; (line = reader.readLine()) != null; ) {
                        if (line.startsWith("RESULT,")) {
                            results.add(line.substring("RESULT,".length()));
                        } else {
                            System.out.println(line);
                        }
                    }
                }
                if (child.waitFor() != 0) {
                    throw new IllegalStateException("Benchmark fork " + fork + " failed");
                }
            } finally {
                Files.deleteIfExists(launchArguments);
            }
            Files.write(report, results, StandardCharsets.UTF_8);
        }
        printSummary(results);
        System.out.println("Raw samples: " + report.toAbsolutePath());
    }

    private static void runFork(final int fork, final String mode) {
        final List<Scenario> scenarios = mode.equals("worldgen3") ? WorldgenRoundThreeScenarios.create()
            : mode.equals("worldgen2") ? WorldgenRoundTwoScenarios.create()
            : mode.equals("worldgen") ? WorldgenHotspotScenarios.create()
            : mode.equals("ticks") ? TickHotspotScenarios.create() : mode.equals("reuse") ? List.of(
            candidateScenario("reuse_1_build", new int[][]{{5, 12}}, 0, true),
            candidateScenario("reuse_1_shuffle3", new int[][]{{5, 12}}, 3, true),
            candidateScenario("reuse_8_overlap_shuffle3", players(8, 1), 3, true),
            candidateScenario("reuse_8_scattered_shuffle3", players(8, 32), 3, true),
            shuffleScenario("reuse_shuffle_only_225", new int[][]{{5, 12}}),
            shuffleScenario("reuse_shuffle_only_1800", players(8, 32))
        ) : List.of(
            candidateScenario("candidates_1_build", new int[][]{{5, 12}}, 0),
            candidateScenario("candidates_1_shuffle3", new int[][]{{5, 12}}, 3),
            candidateScenario("candidates_8_overlap_shuffle3", players(8, 1), 3),
            candidateScenario("candidates_8_scattered_shuffle3", players(8, 32), 3),
            countScenario("counts_378_profile_3types", profileEntities(), 3),
            countScenario("counts_378_profile_4types", profileEntities(), 4),
            countScenario("counts_378_below_cap", entities(378, 35, 20, 7, 2), 3),
            countScenario("counts_10000_over_cap", entities(10_000, 2000, 2000, 2000, 2000), 3)
        );
        final ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported() || !bean.isCurrentThreadCpuTimeSupported()) {
            throw new IllegalStateException("This JVM does not expose allocation and CPU-time counters");
        }
        bean.setThreadAllocatedMemoryEnabled(true);
        bean.setThreadCpuTimeEnabled(true);
        for (final Scenario scenario : scenarios) {
            System.out.println("Fork " + (fork + 1) + "/" + FORKS + ": " + scenario.name());
            scenario.verify();
            final int[] batches = {32, 32};
            final Workload[] workloads = {scenario.vanilla(), scenario.optimized()};
            for (int round = -WARMUP_ROUNDS; round < MEASUREMENT_ROUNDS; round++) {
                for (int turn = 0; turn < workloads.length; turn++) {
                    // Alternate order across rounds and JVMs so one variant does not always run first.
                    final int variant = (turn + round + WARMUP_ROUNDS + fork) & 1;
                    final Measurement measurement = measure(workloads[variant], batches[variant], bean);
                    batches[variant] = Math.clamp((int) (500_000.0 / measurement.nsPerOp()), 1, 8192);
                    if (round >= 0) {
                        System.out.printf(Locale.ROOT, "RESULT,%d,%s,%s,%d,%d,%.6f,%.6f,%.6f%n", fork,
                            scenario.name(), variant == 0 ? scenario.baselineName() : "optimized", round, measurement.operations(),
                            measurement.nsPerOp(), measurement.cpuNsPerOp(), measurement.bytesPerOp());
                    }
                }
            }
        }
    }

    private static Measurement measure(final Workload workload, final int batch, final ThreadMXBean bean) {
        final long thread = Thread.currentThread().threadId();
        final long allocationStart = bean.getThreadAllocatedBytes(thread);
        final long cpuStart = bean.getCurrentThreadCpuTime();
        final long start = System.nanoTime();
        long operations = 0;
        long checksum = 0;
        long elapsed;
        do {
            for (int i = 0; i < batch; i++) {
                checksum += workload.step();
            }
            operations += batch;
            elapsed = System.nanoTime() - start;
        } while (elapsed < ROUND_NANOS);
        final long cpu = bean.getCurrentThreadCpuTime() - cpuStart;
        final long allocation = bean.getThreadAllocatedBytes(thread) - allocationStart;
        blackhole = checksum;
        return new Measurement(operations, (double) elapsed / operations, (double) cpu / operations, (double) allocation / operations);
    }

    private static Scenario candidateScenario(final String name, final int[][] players, final int shuffles) {
        return candidateScenario(name, players, shuffles, false);
    }

    private static Scenario candidateScenario(final String name, final int[][] players, final int shuffles,
                                              final boolean reuseComparison) {
        final Long2ObjectOpenHashMap<SentChunk> loaded = new Long2ObjectOpenHashMap<>();
        for (final int[] player : players) {
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    final int x = player[0] + dx;
                    final int z = player[1] + dz;
                    loaded.put(pack(x, z), new SentChunk(true));
                }
            }
        }
        final SpawnChunkCandidates.ChunkFilter filter = (x, z) -> {
            final SentChunk chunk = loaded.get(pack(x, z));
            return chunk != null && chunk.sent();
        };
        final Workload baseline = reuseComparison ? new BufferedCandidates(players, filter, shuffles)
            : new VanillaCandidates(players, filter, shuffles);
        final OptimizedCandidates optimized = new OptimizedCandidates(players, filter, shuffles);
        return new Scenario(name, baseline, optimized, () -> {
            baseline.step();
            optimized.step();
            final Set<Long> expected = new HashSet<>();
            final int capCount;
            if (baseline instanceof BufferedCandidates buffered) {
                for (int i = 0; i < buffered.positions.size(); i++) {
                    expected.add(buffered.positions.getLong(i));
                }
                capCount = buffered.capCount;
            } else {
                final VanillaCandidates vanilla = (VanillaCandidates) baseline;
                for (final ChunkPos position : vanilla.eligible) {
                    expected.add(pack(position.x, position.z));
                }
                capCount = vanilla.capCount;
            }
            final Set<Long> actual = new HashSet<>();
            final long[] values = optimized.candidates.shuffle(new Random(0));
            for (int i = 0; i < optimized.candidates.size(); i++) {
                actual.add(values[i]);
            }
            if (!expected.equals(actual) || capCount != optimized.candidates.capChunkCount()) {
                throw new AssertionError("Candidate sets or mob-cap counts differ: " + name);
            }
        }, reuseComparison ? "buffer_copy" : "vanilla");
    }

    private static Scenario shuffleScenario(final String name, final int[][] players) {
        final SpawnChunkCandidates.ChunkFilter filter = (x, z) -> true;
        final BufferedCandidates buffered = new BufferedCandidates(players, filter, 0);
        buffered.step();
        final SpawnChunkCandidates candidates = new SpawnChunkCandidates();
        for (final int[] player : players) {
            candidates.addPlayer(player[0], player[1], filter);
        }
        final Random random = new Random(42);
        final Workload baseline = () -> {
            final long[] values = buffered.shuffle();
            long checksum = 0;
            for (int i = 0; i < buffered.positions.size(); i++) {
                checksum = checksum * 31 + values[i];
            }
            return checksum;
        };
        final Workload optimized = () -> {
            final long[] values = candidates.shuffle(random);
            long checksum = 0;
            for (int i = 0; i < candidates.size(); i++) {
                checksum = checksum * 31 + values[i];
            }
            return checksum;
        };
        return new Scenario(name, baseline, optimized, () -> {
            if (baseline.step() != optimized.step()) {
                throw new AssertionError("First shuffle differs for identical input and seed: " + name);
            }
        }, "buffer_copy");
    }

    private static Scenario countScenario(final String name, final List<BenchEntity> entities, final int types) {
        final Class<?>[] classes = {Monster.class, Ambient.class, Water.class, Animal.class};
        final int[] limits = {70, 15, 5, 10};
        final List<Predicate<BenchEntity>> predicates = new ArrayList<>();
        for (int i = 0; i < types; i++) {
            final Class<?> creatureClass = classes[i];
            predicates.add(entity -> !entity.persistent && creatureClass.isAssignableFrom(entity.getClass()));
        }
        final Workload vanilla = () -> {
            long allowed = 0;
            for (int type = 0; type < types; type++) {
                int count = 0;
                for (int i = 0; i < entities.size(); i++) {
                    if (predicates.get(type).test(entities.get(i))) {
                        count++;
                    }
                }
                if (count <= limits[type]) {
                    allowed |= 1L << type;
                }
            }
            return allowed;
        };
        final Workload optimized = () -> {
            long allowed = 0;
            for (int type = 0; type < types; type++) {
                if (SpawnCountLimiter.isWithinLimit(entities, predicates.get(type), limits[type])) {
                    allowed |= 1L << type;
                }
            }
            return allowed;
        };
        return new Scenario(name, vanilla, optimized, () -> {
            if (vanilla.step() != optimized.step()) {
                throw new AssertionError("Spawn-cap decisions differ: " + name);
            }
            int optimizedCalls = 0;
            for (int type = 0; type < types; type++) {
                int count = 0;
                for (final BenchEntity entity : entities) {
                    optimizedCalls++;
                    if (predicates.get(type).test(entity) && ++count > limits[type]) {
                        break;
                    }
                }
            }
            System.out.println("  Classification calls/op: vanilla=" + entities.size() * types + ", optimized=" + optimizedCalls);
        });
    }

    private static int[][] players(final int count, final int spacing) {
        final int[][] result = new int[count][2];
        for (int i = 0; i < count; i++) {
            result[i][0] = 5 + i * spacing;
            result[i][1] = 12 + (i & 1) * spacing;
        }
        return result;
    }

    private static List<BenchEntity> profileEntities() {
        // Profile totals: 72 monsters, 205 animals, 17 ambient, 8 water creatures, 76 other entities.
        return entities(378, 72, 205, 17, 8);
    }

    private static List<BenchEntity> entities(final int total, final int monsters, final int animals,
                                              final int ambient, final int water) {
        final List<BenchEntity> entities = new ArrayList<>(total);
        for (int i = 0; i < monsters; i++) {
            entities.add(new Monster());
        }
        for (int i = 0; i < animals; i++) {
            entities.add(new Animal());
        }
        for (int i = 0; i < ambient; i++) {
            entities.add(new Ambient());
        }
        for (int i = 0; i < water; i++) {
            entities.add(new Water());
        }
        while (entities.size() < total) {
            entities.add(new BenchEntity());
        }
        Collections.shuffle(entities, new Random(0x77192L));
        return entities;
    }

    private static void printSummary(final List<String> rows) {
        final List<String> names = new ArrayList<>();
        for (final String row : rows.subList(1, rows.size())) {
            final String name = row.split(",")[1];
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        System.out.println("scenario,baseline,baseline_median_ns,optimized_median_ns,speedup,baseline_bytes,optimized_bytes");
        for (final String name : names) {
            final String baselineName = rows.stream().skip(1).map(row -> row.split(","))
                                            .filter(columns -> columns[1].equals(name) && !columns[2].equals("optimized"))
                                            .findFirst().orElseThrow()[2];
            final double[][] medians = new double[2][2];
            for (int variant = 0; variant < 2; variant++) {
                final String variantName = variant == 0 ? baselineName : "optimized";
                final List<double[]> perFork = new ArrayList<>();
                for (int fork = 0; fork < FORKS; fork++) {
                    final List<Double> times = new ArrayList<>();
                    final List<Double> allocations = new ArrayList<>();
                    for (final String row : rows.subList(1, rows.size())) {
                        final String[] columns = row.split(",");
                        if (Integer.parseInt(columns[0]) == fork && columns[1].equals(name) && columns[2].equals(variantName)) {
                            times.add(Double.parseDouble(columns[5]));
                            allocations.add(Double.parseDouble(columns[7]));
                        }
                    }
                    perFork.add(new double[]{median(times), median(allocations)});
                }
                medians[variant][0] = median(perFork.stream().map(values -> values[0]).toList());
                medians[variant][1] = median(perFork.stream().map(values -> values[1]).toList());
            }
            System.out.printf(Locale.ROOT, "%s,%s,%.1f,%.1f,%.3f,%.1f,%.1f%n", name, baselineName,
                medians[0][0], medians[1][0], medians[0][0] / medians[1][0], medians[0][1], medians[1][1]);
        }
    }

    private static double median(final List<Double> values) {
        final double[] sorted = values.stream().mapToDouble(Double::doubleValue).toArray();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static long pack(final int x, final int z) {
        return (x & 0xFFFFFFFFL) | ((long) z << 32);
    }

    @FunctionalInterface
    interface Workload {
        long step();
    }

    private record Measurement(long operations, double nsPerOp, double cpuNsPerOp, double bytesPerOp) {
    }

    record Scenario(String name, Workload vanilla, Workload optimized, Runnable verification, String baselineName) {
        Scenario(final String name, final Workload vanilla, final Workload optimized, final Runnable verification) {
            this(name, vanilla, optimized, verification, "vanilla");
        }

        private void verify() {
            this.verification.run();
        }
    }

    private record SentChunk(boolean sent) {
    }

    private static final class VanillaCandidates implements Workload {
        private final Set<ChunkPos> eligible = new HashSet<>();
        private final Random random = new Random(42);
        private final int[][] players;
        private final SpawnChunkCandidates.ChunkFilter filter;
        private final int shuffles;
        private int capCount;

        private VanillaCandidates(final int[][] players, final SpawnChunkCandidates.ChunkFilter filter, final int shuffles) {
            this.players = players;
            this.filter = filter;
            this.shuffles = shuffles;
        }

        @Override
        public long step() {
            this.eligible.clear();
            this.capCount = 0;
            for (final int[] player : this.players) {
                for (int dx = -8; dx <= 8; dx++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        final ChunkPos pos = new ChunkPos(player[0] + dx, player[1] + dz);
                        if (!this.eligible.contains(pos)) {
                            this.capCount++;
                            if (dx != -8 && dx != 8 && dz != -8 && dz != 8 && this.filter.test(pos.x, pos.z)) {
                                this.eligible.add(pos);
                            }
                        }
                    }
                }
            }
            long checksum = (long) this.capCount * 31 + this.eligible.size();
            for (int type = 0; type < this.shuffles; type++) {
                final List<ChunkPos> shuffled = new ArrayList<>(this.eligible);
                Collections.shuffle(shuffled, this.random);
                for (final ChunkPos position : shuffled) {
                    checksum = checksum * 31 + pack(position.x, position.z);
                }
            }
            return checksum;
        }
    }

    // Keep this baseline independent of production changes so comparisons remain reproducible.
    private static final class BufferedCandidates implements Workload {
        private final LongOpenHashSet eligible = new LongOpenHashSet(225);
        private final LongArrayList positions = new LongArrayList(225);
        private final Random random = new Random(42);
        private final int[][] players;
        private final SpawnChunkCandidates.ChunkFilter filter;
        private final int shuffles;
        private long[] shuffled = new long[225];
        private int capCount;

        private BufferedCandidates(final int[][] players, final SpawnChunkCandidates.ChunkFilter filter, final int shuffles) {
            this.players = players;
            this.filter = filter;
            this.shuffles = shuffles;
        }

        @Override
        public long step() {
            this.eligible.clear();
            this.positions.clear();
            this.capCount = 0;
            for (final int[] player : this.players) {
                this.addPlayer(player[0], player[1]);
            }
            long checksum = (long) this.capCount * 31 + this.positions.size();
            for (int type = 0; type < this.shuffles; type++) {
                final long[] values = this.shuffle();
                for (int i = 0; i < this.positions.size(); i++) {
                    checksum = checksum * 31 + values[i];
                }
            }
            return checksum;
        }

        private void addPlayer(final int centerX, final int centerZ) {
            if (this.eligible.isEmpty()) {
                this.capCount += 289;
                for (int dx = -7; dx <= 7; dx++) {
                    for (int dz = -7; dz <= 7; dz++) {
                        final int x = centerX + dx;
                        final int z = centerZ + dz;
                        if (this.filter.test(x, z)) {
                            final long key = pack(x, z);
                            this.eligible.add(key);
                            this.positions.add(key);
                        }
                    }
                }
                return;
            }
            for (int dx = -8; dx <= 8; dx++) {
                final int x = centerX + dx;
                for (int dz = -8; dz <= 8; dz++) {
                    final int z = centerZ + dz;
                    final long key = pack(x, z);
                    if (!this.eligible.contains(key)) {
                        this.capCount++;
                        if (dx != -8 && dx != 8 && dz != -8 && dz != 8 && this.filter.test(x, z)) {
                            this.eligible.add(key);
                            this.positions.add(key);
                        }
                    }
                }
            }
        }

        private long[] shuffle() {
            final int size = this.positions.size();
            if (this.shuffled.length < size) {
                this.shuffled = Arrays.copyOf(this.shuffled, Math.max(size, this.shuffled.length * 2));
            }
            System.arraycopy(this.positions.elements(), 0, this.shuffled, 0, size);
            for (int i = size; i > 1; i--) {
                final int other = this.random.nextInt(i);
                final long key = this.shuffled[i - 1];
                this.shuffled[i - 1] = this.shuffled[other];
                this.shuffled[other] = key;
            }
            return this.shuffled;
        }
    }

    private static final class OptimizedCandidates implements Workload {
        private final SpawnChunkCandidates candidates = new SpawnChunkCandidates();
        private final Random random = new Random(42);
        private final int[][] players;
        private final SpawnChunkCandidates.ChunkFilter filter;
        private final int shuffles;

        private OptimizedCandidates(final int[][] players, final SpawnChunkCandidates.ChunkFilter filter, final int shuffles) {
            this.players = players;
            this.filter = filter;
            this.shuffles = shuffles;
        }

        @Override
        public long step() {
            this.candidates.clear(this.players.length > 1);
            for (final int[] player : this.players) {
                this.candidates.addPlayer(player[0], player[1], this.filter);
            }
            long checksum = (long) this.candidates.capChunkCount() * 31 + this.candidates.size();
            for (int type = 0; type < this.shuffles; type++) {
                final long[] shuffled = this.candidates.shuffle(this.random);
                for (int i = 0; i < this.candidates.size(); i++) {
                    checksum = checksum * 31 + shuffled[i];
                }
            }
            return checksum;
        }
    }

    private static class BenchEntity {
        private boolean persistent;
    }

    private static final class Monster extends BenchEntity {
    }

    private static final class Animal extends BenchEntity {
    }

    private static final class Ambient extends BenchEntity {
    }

    private static final class Water extends BenchEntity {
    }
}
