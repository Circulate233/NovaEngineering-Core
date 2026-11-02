package github.kasuminova.novaeng.mixin.chisel;

import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import team.chisel.Features;
import team.chisel.common.config.Configurations;
import team.chisel.common.util.GenerationHandler;

@Mixin(value = GenerationHandler.class, remap = false)
public abstract class MixinBasaltScan {
    @Shadow
    private void replace(final World world, final BlockPos pos) {
        throw new AssertionError();
    }

    /**
     * @author NovaEngineering
     * @reason Reuse horizontal and vertical probes while retaining every overlapping check, placement and callback in order.
     */
    @Overwrite
    public void onLavaLakes(final PopulateChunkEvent.Post event) {
        if (!Configurations.basaltSpecialGen || !Features.BASALT.enabled()) return;
        final BlockPos origin = new BlockPos(event.getChunkX() * 16 + 8, 0, event.getChunkZ() * 16 + 8);
        final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (final BlockPos pos : BlockPos.getAllInBoxMutable(origin, origin.add(15, 11, 15))) {
            if (event.getWorld().getBlockState(pos).getMaterial() != Material.LAVA) continue;
            final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            final World world = event.getWorld();
            final int size = Configurations.basaltSideThickness;
            final int minX = Math.min(x - size, x + size), maxX = Math.max(x - size, x + size);
            final int minZ = Math.min(z - size, z + size), maxZ = Math.max(z - size, z + size);
            for (int dz = minZ; ; dz++) {
                for (int dx = minX; ; dx++) {
                    replace(world, probe.setPos(dx, y, dz));
                    if (dx == maxX) break;
                }
                if (dz == maxZ) break;
            }
            for (int dy = 1; dy <= Configurations.basaltBottomThickness; dy++) {
                replace(world, probe.setPos(x, y - dy, z));
            }
        }
    }
}
