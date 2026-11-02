package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.LocalChunkReads;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(World.class)
public abstract class MixinWorldCollisionReads {
    /**
     * @author NovaEngineering
     * @reason Reuse chunks during callback-free air scans and invalidate around real collision callbacks and Forge events.
     */
    @Overwrite
    public boolean getCollisionBoxes(Entity entityIn, AxisAlignedBB aabb, boolean checkOnly, @Nullable List<AxisAlignedBB> outList) {
        final World world = (World) (Object) this;
        try (final LocalChunkReads reads = LocalChunkReads.borrow(world)) {
            int minX = MathHelper.floor(aabb.minX) - 1;
            int maxX = MathHelper.ceil(aabb.maxX) + 1;
            int minY = MathHelper.floor(aabb.minY) - 1;
            int maxY = MathHelper.ceil(aabb.maxY) + 1;
            int minZ = MathHelper.floor(aabb.minZ) - 1;
            int maxZ = MathHelper.ceil(aabb.maxZ) + 1;
            WorldBorder border = world.getWorldBorder();
            if (border.getClass() != WorldBorder.class) reads.disable();
            boolean outsideBorder = entityIn != null && entityIn.isOutsideBorder();
            boolean insideBorder = entityIn != null && world.isInsideWorldBorder(entityIn);
            IBlockState borderState = Blocks.STONE.getDefaultState();
            BlockPos.PooledMutableBlockPos probe = BlockPos.PooledMutableBlockPos.retain();
            if (checkOnly && !ForgeEventFactory.gatherCollisionBoxes(world, entityIn, aabb, outList)) {
                return true;
            } else {
                try {
                    for (int x = minX; x < maxX; ++x) {
                        for (int z = minZ; z < maxZ; ++z) {
                            boolean edgeX = x == minX || x == maxX - 1;
                            boolean edgeZ = z == minZ || z == maxZ - 1;
                            if ((!edgeX || !edgeZ) && world.isBlockLoaded(probe.setPos(x, 64, z))) {
                                for (int y = minY; y < maxY; ++y) {
                                    if (!edgeX && !edgeZ || y != maxY - 1) {
                                        if (checkOnly) {
                                            if (x < -30000000 || x >= 30000000 || z < -30000000 || z >= 30000000) {
                                                return true;
                                            }
                                        } else if (entityIn != null && outsideBorder == insideBorder) {
                                            reads.invalidate();
                                            entityIn.setOutsideBorder(!insideBorder);
                                        }

                                        probe.setPos(x, y, z);
                                        IBlockState state;
                                        if (!checkOnly && !border.contains(probe) && insideBorder) {
                                            state = borderState;
                                        } else {
                                            state = reads.get(probe);
                                        }

                                        state.addCollisionBoxToList(world, probe, aabb, outList, entityIn, false);
                                        if (checkOnly || state != Blocks.AIR.getDefaultState()) reads.invalidate();
                                        if (checkOnly && !ForgeEventFactory.gatherCollisionBoxes(world, entityIn, aabb, outList)) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    probe.release();
                }

                return !outList.isEmpty();
            }
        }
    }

}
