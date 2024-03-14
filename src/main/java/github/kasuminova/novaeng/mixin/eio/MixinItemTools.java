package github.kasuminova.novaeng.mixin.eio;

import crazypants.enderio.base.capability.ItemTools;
import net.minecraft.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemTools.class)
public class MixinItemTools {

    @Redirect(
            method = "move(Lcrazypants/enderio/base/capability/ItemTools$Limit;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Lcrazypants/enderio/base/capability/ItemTools$MoveResult;",
            at = @At(value = "INVOKE", target = "Lcrazypants/enderio/base/diagnostics/Prof;start(Lnet/minecraft/profiler/Profiler;Ljava/lang/String;Ljava/lang/Object;)V"),
            remap = false
    )
    private static void redirectProfStart(final Profiler profiler, final String section, final Object param) {
        // noop
    }

    @Redirect(
            method = "move(Lcrazypants/enderio/base/capability/ItemTools$Limit;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Lcrazypants/enderio/base/capability/ItemTools$MoveResult;",
            at = @At(value = "INVOKE", target = "Lcrazypants/enderio/base/diagnostics/Prof;stop(Lnet/minecraft/profiler/Profiler;)V"),
            remap = false
    )
    private static void redirectProfStop(final Profiler profiler) {
        // noop
    }

    @Redirect(
            method = "move(Lcrazypants/enderio/base/capability/ItemTools$Limit;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Lcrazypants/enderio/base/capability/ItemTools$MoveResult;",
            at = @At(value = "INVOKE", target = "Lcrazypants/enderio/base/diagnostics/Prof;stop(Lnet/minecraft/profiler/Profiler;I)V"),
            remap = false
    )
    private static void redirectProfStop(final Profiler i, final int profiler) {
        // noop
    }

}
