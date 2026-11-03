package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.CollisionBoxQueries;
import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(Block.class)
public abstract class MixinBlockCollisionAllocation {
    /**
     * @author NovaEngineering
     * @reason Allocate translated collision boxes only when they intersect the query.
     */
    @Overwrite
    protected static void addCollisionBoxToList(final BlockPos pos, final AxisAlignedBB entityBox,
                                                final List<AxisAlignedBB> boxes, final AxisAlignedBB blockBox) {
        CollisionBoxQueries.append(pos, entityBox, boxes, blockBox);
    }
}
