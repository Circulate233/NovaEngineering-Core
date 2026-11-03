package github.kasuminova.novaeng.common.performance;

import com.sun.management.ThreadMXBean;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class JavaOptimizationBenchmark {
    private static final ThreadMXBean BEAN = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static volatile long sink;

    private JavaOptimizationBenchmark() {
    }

    static void main(final String[] args) throws Throwable {
        Bootstrap.register();
        BEAN.setThreadAllocatedMemoryEnabled(true);
        BEAN.setThreadCpuTimeEnabled(true);
        final StringBuilder report = new StringBuilder("scenario,version,cpu_ns_per_op,bytes_per_op,checksum\n");
        for (final int unique : new int[]{8, 64, 512}) {
            final ItemStack[] input = stacks(unique, false);
            compare(report, "dedup_items_" + unique, () -> dedup(input, false), () -> dedup(input, true));
        }
        final ItemStack[] variants = stacks(512, true);
        compare(report, "dedup_same_item_512", () -> dedup(variants, false), () -> dedup(variants, true));
        final byte[] values = new byte[256];
        for (int i = 0; i < values.length; i++) {
            values[i] = (byte) i;
        }
        compare(report, "drill_pow_256", () -> powers(values, false), () -> powers(values, true));
        Files.writeString(Path.of(args[0]), report);
        System.out.print(report);
        System.out.println("checksum=" + sink);
    }

    private static ItemStack[] stacks(final int unique, final boolean sameItem) {
        final Item[] items = new Item[unique];
        for (int i = 0; i < unique; i++) {
            items[i] = sameItem ? Items.APPLE : new Item();
        }
        final ItemStack[] result = new ItemStack[unique * 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = new ItemStack(items[i % unique], 1 + i % 8, i % unique);
            final NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("variant", i % unique);
            result[i].setTagCompound(tag);
        }
        return result;
    }

    private static long dedup(final ItemStack[] input, final boolean indexed) {
        if (!indexed) {
            return dedupLinear(input);
        }
        boolean diverse = false;
        if (input.length >= ItemStackIndex.INDEX_THRESHOLD) {
            for (int i = 1; i < ItemStackIndex.INDEX_THRESHOLD; i++) {
                if (input[i].getItem() != input[0].getItem()) {
                    diverse = true;
                    break;
                }
            }
        }
        if (!diverse) {
            return dedupLinear(input);
        }
        final List<ItemStack> stacks = new ObjectArrayList<>();
        long checksum = 0;
        try (final ItemStackIndex index = ItemStackIndex.open(stacks, false)) {
            for (final ItemStack stack : input) {
                checksum += index.findOrAdd(stack);
            }
        }
        return checksum;
    }

    private static long dedupLinear(final ItemStack[] input) {
        final List<ItemStack> stacks = new ObjectArrayList<>();
        long checksum = 0;
        for (final ItemStack stack : input) {
            int found = JavaOptimizationTest.linearFind(stacks, stack, false);
            if (found < 0) {
                found = stacks.size();
                stacks.add(stack);
            }
            checksum += found;
        }
        return checksum;
    }

    private static long powers(final byte[] values, final boolean indexed) {
        double sum = 0;
        for (final byte value : values) {
            if (value > 0) {
                sum += indexed ? DrillOutputLookup.researchDamage(value) * DrillOutputLookup.componentMultiplier(value)
                    : 1000 * Math.pow(value, 1.6) * (1 + Math.pow(value, 2.5));
            }
        }
        return Double.doubleToLongBits(sum);
    }

    private static void compare(final StringBuilder out, final String name, final Task original, final Task optimized) throws Throwable {
        if (original.run() != optimized.run()) {
            throw new IllegalStateException("Benchmark results differ: " + name);
        }
        final Sample[][] samples = {new Sample[9], new Sample[9]};
        final Task[] tasks = {original, optimized};
        for (int round = -6; round < 9; round++) {
            for (int turn = 0; turn < 2; turn++) {
                final int version = (round + 6 + turn) & 1;
                final Sample sample = measure(tasks[version]);
                if (round >= 0) {
                    samples[version][round] = sample;
                }
            }
        }
        for (int version = 0; version < 2; version++) {
            final double[] cpu = new double[9];
            final double[] allocated = new double[9];
            for (int i = 0; i < 9; i++) {
                cpu[i] = samples[version][i].cpu;
                allocated[i] = samples[version][i].allocated;
            }
            Arrays.sort(cpu);
            Arrays.sort(allocated);
            out.append(name).append(',').append(version == 0 ? "original" : "optimized").append(',')
               .append(cpu[4]).append(',').append(allocated[4]).append(',').append(samples[version][4].checksum).append('\n');
        }
    }

    private static Sample measure(final Task task) throws Throwable {
        final long thread = Thread.currentThread().threadId();
        final long allocated = BEAN.getThreadAllocatedBytes(thread);
        final long cpu = BEAN.getCurrentThreadCpuTime();
        final long start = System.nanoTime();
        long count = 0;
        long checksum = 0;
        do {
            for (int i = 0; i < 32; i++) {
                checksum += task.run();
            }
            count += 32;
        } while (BEAN.getCurrentThreadCpuTime() - cpu < 125_000_000L
            && System.nanoTime() - start < 2_000_000_000L);
        final long elapsed = BEAN.getCurrentThreadCpuTime() - cpu;
        final long bytes = BEAN.getThreadAllocatedBytes(thread) - allocated;
        sink = checksum;
        return new Sample((double) elapsed / count, (double) bytes / count, checksum);
    }

    @FunctionalInterface
    private interface Task {
        long run() throws Throwable;
    }

    private record Sample(double cpu, double allocated, long checksum) {
    }
}
