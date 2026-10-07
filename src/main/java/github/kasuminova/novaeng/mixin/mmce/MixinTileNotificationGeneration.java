package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.performance.DirtyNotificationAccess;
import github.kasuminova.novaeng.common.performance.DirtyNotificationState;
import github.kasuminova.novaeng.common.performance.PerformanceMetrics;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileEntitySynchronized.class, remap = false)
public abstract class MixinTileNotificationGeneration implements DirtyNotificationAccess {
    @Unique
    private final DirtyNotificationState nova$notifications = new DirtyNotificationState();
    @Shadow
    private boolean inMarkTask;
    @Shadow
    private boolean inUpdateTask;

    @Override
    public DirtyNotificationState nova$notificationState() {
        return nova$notifications;
    }

    @Redirect(method = "markNoUpdateSync", at = @At(value = "FIELD",
        target = "Lhellfirepvp/modularmachinery/common/tiles/base/TileEntitySynchronized;inMarkTask:Z",
        opcode = Opcodes.GETFIELD), require = 1)
    private boolean nova$recordMarkMutation(final TileEntitySynchronized tile) {
        nova$notifications.changed();
        return inMarkTask;
    }

    @Redirect(method = "markForUpdateSync", at = @At(value = "FIELD",
        target = "Lhellfirepvp/modularmachinery/common/tiles/base/TileEntitySynchronized;inUpdateTask:Z",
        opcode = Opcodes.GETFIELD), require = 1)
    private boolean nova$recordUpdateMutation(final TileEntitySynchronized tile) {
        nova$notifications.changed();
        return inUpdateTask;
    }

    @Redirect(method = "markNoUpdate", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/World;updateComparatorOutputLevel(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;)V",
        remap = true), require = 1)
    private void nova$commitComparator(final World world, final BlockPos pos, final Block blockIn) {
        final long tick = world.getMinecraftServer() == null ? Long.MIN_VALUE : world.getMinecraftServer().getTickCounter();
        final long version = nova$notifications.capture();
        if (!world.isRemote && tick != Long.MIN_VALUE && nova$notifications.isQueued()
            && nova$notifications.alreadyCommitted(version, tick)) {
            PerformanceMetrics.add(PerformanceMetrics.Counter.MMCE_NOTIFICATIONS_SKIPPED, 1);
            return;
        }
        world.updateComparatorOutputLevel(pos, blockIn);
        nova$notifications.committed(version, tick);
        PerformanceMetrics.add(PerformanceMetrics.Counter.MMCE_NOTIFICATIONS, 1);
    }
}
