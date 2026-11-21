package github.kasuminova.novaeng.mixin.neofontrender;

import github.kasuminova.novaeng.client.font.TextRouteHash;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "neofontrender.api.text.route.TextRenderRouteApi$CacheKey", remap = false)
public abstract class MixinTextRouteCacheKeyHash {
    @Shadow
    @Final
    String source;
    @Shadow
    @Final
    int argb;
    @Shadow
    @Final
    boolean shadow;
    @Shadow
    @Final
    int fontSizeBits;
    @Shadow
    @Final
    long revision;
    @Shadow
    @Final
    long structuredRevision;
    @Shadow
    @Final
    int rasterScaleBits;
    @Shadow
    @Final
    int engineBits;
    @Shadow
    @Final
    long backendState;

    /**
     * @author NovaEngineering
     * @reason Preserve the original hash while eliminating the Object[9] and boxed primitives.
     */
    @Overwrite
    public int hashCode() {
        return TextRouteHash.hash(source, argb, shadow, fontSizeBits, revision, structuredRevision,
            rasterScaleBits, engineBits, backendState);
    }
}
