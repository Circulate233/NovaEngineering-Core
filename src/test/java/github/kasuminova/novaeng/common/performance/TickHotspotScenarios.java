package github.kasuminova.novaeng.common.performance;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import github.kasuminova.novaeng.common.performance.SpawnPerformanceBenchmark.Scenario;
import github.kasuminova.novaeng.common.performance.SpawnPerformanceBenchmark.Workload;
import ic2.core.util.Config;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class TickHotspotScenarios {

    private TickHotspotScenarios() {
    }

    static List<Scenario> create() {
        try {
            return List.of(sparseTasks(false), sparseTasks(true), configPath(), dimensions(), leakCounters(),
                chunks(225), chunks(600), chunks(2000));
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Scenario sparseTasks(final boolean activeWorld) {
        final Object[] worlds = {new Object(), new Object(), new Object(), new Object(), new Object()};
        final Multimap<Object, Integer> oldTasks = LinkedHashMultimap.create();
        final Multimap<Object, Integer> newTasks = LinkedHashMultimap.create();
        final Multimap<Object, Integer> pending = LinkedHashMultimap.create();
        if (activeWorld) {
            oldTasks.putAll(worlds[0], List.of(1, 2));
            newTasks.putAll(worlds[0], List.of(1, 2));
        }
        final Predicate<Integer> tick = task -> task < 0;
        final Workload baseline = () -> {
            for (final Object world : worlds) {
                oldTasks.get(world).removeIf(tick);
                oldTasks.putAll(pending);
                pending.clear();
            }
            return oldTasks.size();
        };
        final Workload optimized = () -> {
            for (final Object world : worlds) {
                final Collection<Integer> tasks = SparseTickTasks.existing(newTasks, world);
                if (tasks != null) {
                    tasks.removeIf(tick);
                }
                SparseTickTasks.mergePending(newTasks, pending);
            }
            return newTasks.size();
        };
        return checked(activeWorld ? "zenutils_5worlds_1active" : "zenutils_5worlds_empty", baseline, optimized);
    }

    private static Scenario configPath() throws ReflectiveOperationException {
        final MethodHandle oldRead = configRead(Config.class);
        final MethodHandle newRead = configRead(IC2ConfigPathsTest.transformedConfigLoader().loadClass("ic2.core.util.Config"));
        return checked("ic2_live_config_path", () -> invokeBool(oldRead), () -> invokeBool(newRead));
    }

    private static MethodHandle configRead(final Class<?> configClass) throws ReflectiveOperationException {
        final Object config = configClass.getConstructor(String.class).newInstance("benchmark");
        configClass.getMethod("set", String.class, Object.class).invoke(config, "balance/disableEnderChest", false);
        final MethodHandle get = MethodHandles.publicLookup().unreflect(configClass.getMethod("get", String.class))
                                              .bindTo(config).bindTo("balance/disableEnderChest");
        final MethodHandle bool = MethodHandles.publicLookup().unreflect(get.type().returnType().getMethod("getBool"));
        return MethodHandles.filterReturnValue(get, bool).asType(MethodType.methodType(boolean.class));
    }

    private static long invokeBool(final MethodHandle handle) {
        try {
            return (boolean) handle.invokeExact() ? 1 : 0;
        } catch (final Throwable exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Scenario dimensions() {
        final Int2ObjectMap<Object> worlds = Int2ObjectMaps.synchronize(new Int2ObjectLinkedOpenHashMap<>());
        for (final int id : new int[]{0, -1, 1, 2, 99}) {
            worlds.put(id, new Object());
        }
        return checked("dimension_ids_5", () -> checksum(worlds.keySet().toArray(new Integer[0])),
            () -> checksum(worlds.keySet().toIntArray()));
    }

    private static Scenario leakCounters() {
        final int[] hashes = new int[32];
        for (int i = 0; i < hashes.length; i++) {
            hashes[i] = System.identityHashCode(new Object());
        }
        final Multiset<Integer> boxed = HashMultiset.create();
        final Int2IntOpenHashMap primitive = new Int2IntOpenHashMap();
        final Workload baseline = () -> {
            for (final int hash : hashes) {
                boxed.add(hash);
            }
            long sum = 0;
            for (final int hash : hashes) {
                sum += boxed.count(hash);
            }
            return sum;
        };
        final Workload optimized = () -> {
            for (final int hash : hashes) {
                primitive.addTo(hash, 1);
            }
            long sum = 0;
            for (final int hash : hashes) {
                sum += primitive.get(hash);
            }
            return sum;
        };
        return checked("dimension_leak_counts_32", baseline, optimized);
    }

    private static long checksum(final Integer[] ids) {
        long checksum = 0;
        for (final Integer id : ids) {
            checksum = checksum * 31 + id;
        }
        return checksum;
    }

    private static long checksum(final int[] ids) {
        long checksum = 0;
        for (final int id : ids) {
            checksum = checksum * 31 + id;
        }
        return checksum;
    }

    private static Scenario chunks(final int count) {
        final List<ChunkEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new ChunkEntry(i, i % 7 != 0, i % 3 == 0, i % 5 == 0, i % 2 == 0));
        }
        final Predicate<ChunkEntry> accepts = entry -> entry.present()
            && (entry.needsLighting() || !entry.ticked() || entry.nearPlayer());
        final Function<ChunkEntry, ChunkEntry> value = Function.identity();
        final TickSnapshotPool<ChunkEntry> pool = new TickSnapshotPool<>();
        final Workload baseline = () -> consume((entries.size() > 500 ? entries.parallelStream() : entries.stream())
            .filter(accepts).map(value).collect(Collectors.toCollection(ObjectArrayList::new)).iterator());
        final Workload optimized = () -> consume(pool.select(entries, accepts, value));
        return checked("chunk_snapshot_" + count, baseline, optimized);
    }

    private static long consume(final Iterator<ChunkEntry> iterator) {
        long checksum = 0;
        while (iterator.hasNext()) {
            checksum = checksum * 31 + iterator.next().id();
        }
        return checksum;
    }

    private static Scenario checked(final String name, final Workload baseline, final Workload optimized) {
        return new Scenario(name, baseline, optimized, () -> {
            if (baseline.step() != optimized.step()) {
                throw new AssertionError("Different result: " + name);
            }
        });
    }

    private record ChunkEntry(int id, boolean present, boolean needsLighting, boolean ticked, boolean nearPlayer) {
    }
}
