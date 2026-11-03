package github.kasuminova.novaeng.mixin.reid;

import github.kasuminova.novaeng.common.performance.SectionStorageAccess;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.IBlockStatePalette;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The network reader is stripped from BlockStateContainer on dedicated servers. */
@Mixin(value = BlockStateContainer.class, priority = 500)
public abstract class MixinSectionStorageClient implements SectionStorageAccess {
    @Redirect(
        method = "read(Lnet/minecraft/network/PacketBuffer;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/IBlockStatePalette;read(Lnet/minecraft/network/PacketBuffer;)V"),
        require = 1)
    private void nova$readPalette(final IBlockStatePalette palette, final PacketBuffer buffer) {
        this.nova$invalidatePaletteSetCache();
        palette.read(buffer);
    }
}
