package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.common.performance.ChunkNbtRetention;
import net.minecraft.nbt.NBTTagCompound;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraftforge.common.chunkio.ChunkIOProvider", remap = false)
public abstract class MixinChunkIOProviderNbt {
    @Shadow private NBTTagCompound nbt;
    @Unique private ChunkNbtRetention nova$pendingNbt;

    // This store is inside run()'s monitor, before ran=true publishes the result.
    @Redirect(method = "run", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lnet/minecraftforge/common/chunkio/ChunkIOProvider;nbt:Lnet/minecraft/nbt/NBTTagCompound;"), require = 1)
    private void nova$retainNbt(final @Coerce Object provider, final NBTTagCompound original) {
        nova$pendingNbt = ChunkNbtRetention.capture(original);
        nbt = nova$pendingNbt == null ? original : nova$pendingNbt.retainedData();
    }

    // Restore before loadEntities AND ChunkDataEvent.Load, on the original callback thread.
    @Inject(method = "syncCallback", at = @At("HEAD"), require = 1)
    private void nova$restoreNbt(final CallbackInfo ci) {
        if (nova$pendingNbt != null) {
            nbt = nova$pendingNbt.restore();
            nova$pendingNbt = null;
        }
    }
}
