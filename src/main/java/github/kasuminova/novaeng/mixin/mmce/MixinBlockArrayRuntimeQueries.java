package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.performance.mmce.StructureQueries;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;

@Mixin(value = BlockArray.class, remap = false)
public abstract class MixinBlockArrayRuntimeQueries {
    @Shadow protected Map<BlockPos, BlockArray.BlockInformation> pattern;

    /**
     * @author NovaEngineering
     * @reason Iterate packed backing entries directly, retaining order and immutable World query positions.
     */
    @Overwrite
    public boolean matches(final World world, final BlockPos center, final boolean oldState,
                           final Map<BlockPos, List<BlockArray.BlockInformation>> replacements) {
        return StructureQueries.matches(pattern, world, center, oldState, replacements);
    }
}
