package github.kasuminova.novaeng.mixin.ae2;

import appeng.core.sync.GuiBridge;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GuiBridge.class, remap = false)
public abstract class MixinGuiBridgeWirelessTerminalGuard {

    @Redirect(
            method = {"getServerGuiElement", "getClientGuiElement"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getTileEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/tileentity/TileEntity;",
                    remap = true))
    private TileEntity novaeng$skipItemHostTileLookup(final World world, final BlockPos pos) {
        return pos.getZ() == Integer.MIN_VALUE ? null : world.getTileEntity(pos);
    }
}
