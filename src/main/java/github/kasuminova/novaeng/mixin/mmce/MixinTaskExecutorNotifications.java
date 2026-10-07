package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.mmce.common.concurrent.TaskExecutor;
import github.kasuminova.novaeng.common.performance.MMCENotifications;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TaskExecutor.class, remap = false)
public abstract class MixinTaskExecutorNotifications {
    @Redirect(method = "updateTileEntity", at = @At(value = "INVOKE",
        target = "Lhellfirepvp/modularmachinery/common/tiles/base/TileEntitySynchronized;markNoUpdate()V"), require = 1)
    private void nova$queuedMark(final TileEntitySynchronized tile) {
        MMCENotifications.commit(tile, false);
    }

    @Redirect(method = "updateTileEntity", at = @At(value = "INVOKE",
        target = "Lhellfirepvp/modularmachinery/common/tiles/base/TileEntitySynchronized;markForUpdate()V"), require = 1)
    private void nova$queuedUpdate(final TileEntitySynchronized tile) {
        MMCENotifications.commit(tile, true);
    }
}
