package github.kasuminova.novaeng.mixin.enumcache.moreoverlays;

import at.feldim2425.moreoverlays.chunkbounds.ChunkBoundsHandler.RenderMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RenderMode.class, remap = false)
public abstract class MixinRenderMode {

    @Shadow
    @Final
    private static RenderMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RenderMode[] values() {
        return $VALUES;
    }
}
