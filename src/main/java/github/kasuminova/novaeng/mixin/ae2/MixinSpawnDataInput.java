package github.kasuminova.novaeng.mixin.ae2;

import github.kasuminova.novaeng.common.performance.MeteorSpawnDataInput;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;

@Mixin(targets = "appeng.core.worlddata.SpawnData", remap = false)
public abstract class MixinSpawnDataInput {

    @Redirect(method = "loadSpawnData", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/nbt/CompressedStreamTools;readCompressed(Ljava/io/InputStream;)Lnet/minecraft/nbt/NBTTagCompound;", remap = true), require = 1)
    private NBTTagCompound nova$bufferCompressedFile(final InputStream is) throws IOException {
        return MeteorSpawnDataInput.read(is);
    }
}
