package github.kasuminova.novaeng.mixin.reid;

import github.kasuminova.novaeng.common.performance.SectionCodec;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AnvilChunkLoader.class, priority = 500)
public abstract class MixinAnvilSectionCodec {
    @Redirect(method = "readChunkFromNBT", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/chunk/BlockStateContainer;setDataFromNBT([BLnet/minecraft/world/chunk/NibbleArray;Lnet/minecraft/world/chunk/NibbleArray;)V"), require = 1)
    private void nova$decodeSection(final BlockStateContainer storage, final byte[] blockIds,
                                    final NibbleArray data, final NibbleArray blockIdExtension) {
        SectionCodec.decode(storage, blockIds, data, blockIdExtension);
    }

    @Redirect(method = "writeChunkToNBT", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/chunk/BlockStateContainer;getDataForNBT([BLnet/minecraft/world/chunk/NibbleArray;)Lnet/minecraft/world/chunk/NibbleArray;"), require = 1)
    private NibbleArray nova$encodeSection(final BlockStateContainer storage, final byte[] blockIds, final NibbleArray data) {
        return SectionCodec.encode(storage, blockIds, data);
    }
}
