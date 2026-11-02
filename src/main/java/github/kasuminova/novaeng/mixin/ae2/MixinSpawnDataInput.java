package github.kasuminova.novaeng.mixin.ae2;

import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.common.performance.MeteorRegionCache;
import github.kasuminova.novaeng.common.performance.MeteorSpawnDataInput;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Mixin(targets = "appeng.core.worlddata.SpawnData", remap = false)
public abstract class MixinSpawnDataInput {

    @Unique
    private final MeteorRegionCache nova$regions = new MeteorRegionCache(getClass());

    @Inject(method = "loadSpawnData", at = @At(value = "INVOKE", target = "Ljava/io/File;isFile()Z"),
        cancellable = true, require = 1)
    private void nova$readCachedRegion(final int dimension, final int x, final int z,
                                       final CallbackInfoReturnable<NBTTagCompound> cir, @Local final File file) {
        final NBTTagCompound cached = nova$regions.get(file);
        if (cached != null) cir.setReturnValue(cached);
    }

    @Redirect(method = "loadSpawnData", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/nbt/CompressedStreamTools;readCompressed(Ljava/io/InputStream;)Lnet/minecraft/nbt/NBTTagCompound;", remap = true), require = 1)
    private NBTTagCompound nova$bufferCompressedFile(final InputStream is, @Local final File file) throws IOException {
        final NBTTagCompound data = MeteorSpawnDataInput.read(is);
        nova$regions.completeRead(file, data);
        return data;
    }

    @Inject(method = "writeSpawnData", at = @At(value = "NEW", target = "java/io/FileOutputStream"),
        cancellable = true, require = 1)
    private void nova$prepareRegionWrite(final int dimension, final int x, final int z, final NBTTagCompound data,
                                         final CallbackInfo ci, @Local final File file) throws IOException {
        // An identical successful write has no dirty interval to flush or lose at shutdown.
        if (!nova$regions.containsUnchanged(file, data) && !nova$regions.stage(file, data)) {
            nova$regions.writeThrough(file, data);
        }
        ci.cancel();
    }
}
