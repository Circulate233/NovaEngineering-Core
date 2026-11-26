package github.kasuminova.novaeng.common.performance.mmce;

import github.kasuminova.novaeng.mixin.mmce.AccessorBlockPos2ValueMap;
import github.kasuminova.mmce.common.util.BlockPos2ValueMap;
import hellfirepvp.modularmachinery.common.util.BlockArray.BlockInformation;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;

public final class StructureQueries {
    private StructureQueries() {
    }

    public static boolean matches(final Map<BlockPos, BlockInformation> pattern, final World world,
                                  final BlockPos center, final boolean oldState,
                                  final Map<BlockPos, List<BlockInformation>> replacements) {
        if (pattern.getClass() == BlockPos2ValueMap.class && pattern instanceof AccessorBlockPos2ValueMap<?> access) {
            @SuppressWarnings("unchecked")
            final var internal = ((AccessorBlockPos2ValueMap<BlockInformation>) access).nova$internal();
            return matchesPacked(internal, world, center, oldState, replacements);
        } else {
            // Preserve externally supplied map implementations and their iteration order.
            for (final var entry : pattern.entrySet()) {
                if (!matchesOne(entry.getValue(), entry.getKey(), world, center, oldState, replacements)) return false;
            }
        }
        return true;
    }

    public static boolean matchesPacked(final Long2ObjectMap<BlockInformation> internal, final World world,
                                        final BlockPos center, final boolean oldState,
                                        final Map<BlockPos, List<BlockInformation>> replacements) {
        // Same backing-table order as BlockPos2ValueMap.entrySet(), without two entry wrappers.
        final var entries = Long2ObjectMaps.fastIterator(internal);
        while (entries.hasNext()) {
            final var entry = entries.next();
            final BlockPos relative = BlockPos.fromLong(entry.getLongKey());
            if (!matchesOne(entry.getValue(), relative, world, center, oldState, replacements)) return false;
        }
        return true;
    }

    private static boolean matchesOne(final BlockInformation info, final BlockPos relative, final World world,
                                      final BlockPos center, final boolean oldState,
                                      final Map<BlockPos, List<BlockInformation>> replacements) {
        // Immutable: advanced block checkers and third-party World methods may retain this argument.
        final BlockPos at = center.add(relative);
        if (info.matches(world, at, oldState)) return true;
        if (replacements == null || !replacements.containsKey(relative)) return false;
        for (final BlockInformation replacement : replacements.get(relative)) {
            if (replacement.matches(world, at, oldState)) return true;
        }
        return false;
    }
}
