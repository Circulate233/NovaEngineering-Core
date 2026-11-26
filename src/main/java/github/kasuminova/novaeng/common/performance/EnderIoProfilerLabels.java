package github.kasuminova.novaeng.common.performance;

import crazypants.enderio.util.Profiler;
import github.kasuminova.novaeng.mixin.enderio.AccessorEnderIoProfiler;
import net.minecraft.block.state.IBlockState;

/** Keeps diagnostic labels lazy without caching names across language/resource changes. */
public final class EnderIoProfilerLabels {
    private EnderIoProfilerLabels() {
    }

    public static void stop(final Profiler profiler, final long start, final IBlockState state) {
        if (!((AccessorEnderIoProfiler) profiler).nova$isEnabled()) return;
        profiler.stop(start, state.getBlock().getLocalizedName() + " (relayed)");
    }
}
