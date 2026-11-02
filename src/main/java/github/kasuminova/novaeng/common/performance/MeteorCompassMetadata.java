package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;

import java.util.List;

/**
 * Immutable target metadata with bounded, thread-owned state classifications.
 */
public final class MeteorCompassMetadata {
    private final Reference2ObjectOpenHashMap<ResourceLocation, IntSet> targets =
        new Reference2ObjectOpenHashMap<>();
    private final ThreadLocal<Scan> scans = ThreadLocal.withInitial(Scan::new);

    public MeteorCompassMetadata(final List<IBlockState> configuredStates) {
        for (final IBlockState state : configuredStates) {
            final Block block = state.getBlock();
            final ResourceLocation name = block.getRegistryName();
            IntSet metadata = targets.get(name);
            if (metadata == null) {
                metadata = new IntOpenHashSet();
                targets.put(name, metadata);
            }
            metadata.add(block.getMetaFromState(state));
        }
    }

    public boolean isEmpty() {
        return targets.isEmpty();
    }

    public Scan scan() {
        return scans.get();
    }

    public final class Scan {
        private static final int MAX_CACHED_STATES = 1024;
        private final Reference2ByteOpenHashMap<IBlockState> states =
            new Reference2ByteOpenHashMap<>();

        public boolean matches(final IBlockState state) {
            final byte cached = states.getByte(state);
            if (cached != 0) return cached == 1;
            final Block block = state.getBlock();
            final IntSet metadata = targets.get(block.getRegistryName());
            // Most scanned blocks cannot be targets; never decode their metadata.
            final boolean matches =
                metadata != null && metadata.contains(block.getMetaFromState(state));
            if (states.size() < MAX_CACHED_STATES) states.put(state, (byte) (matches ? 1 : 2));
            return matches;
        }
    }
}
