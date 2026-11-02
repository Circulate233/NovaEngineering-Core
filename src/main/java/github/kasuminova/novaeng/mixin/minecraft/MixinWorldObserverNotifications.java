package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.ObserverNotifications;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(World.class)
public abstract class MixinWorldObserverNotifications {
    /**
     * @author NovaEngineering
     * @reason Skip proven empty observer sections while retaining chunk loading and callback order.
     */
    @Overwrite
    public void observedNeighborChanged(final BlockPos pos, final Block changedBlock, final BlockPos changedBlockPos) {
        ObserverNotifications.notify((World) (Object) this, pos, changedBlock, changedBlockPos);
    }
}
