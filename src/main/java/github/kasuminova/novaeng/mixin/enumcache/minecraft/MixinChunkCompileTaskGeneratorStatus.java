package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ChunkCompileTaskGenerator.Status.class, remap = false)
public abstract class MixinChunkCompileTaskGeneratorStatus {

    @Shadow
    @Final
    private static ChunkCompileTaskGenerator.Status[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ChunkCompileTaskGenerator.Status[] values() {
        return $VALUES;
    }
}
