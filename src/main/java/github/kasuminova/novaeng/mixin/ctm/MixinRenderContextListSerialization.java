package github.kasuminova.novaeng.mixin.ctm;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.api.texture.ITextureContext;
import team.chisel.ctm.api.util.RenderContextList;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Mixin(value = RenderContextList.class, remap = false)
public abstract class MixinRenderContextListSerialization {

    @Shadow
    @Final
    private Object2LongMap<ICTMTexture<?>> serialized;

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
        target = "Ljava/util/Map;entrySet()Ljava/util/Set;"), remap = false, require = 1)
    private Set<Map.Entry<ICTMTexture<?>, ITextureContext>> nova$serializeWithoutEntries(
        final Map<ICTMTexture<?>, ITextureContext> map) {
        map.forEach((texture, context) -> this.serialized.put(texture, context.getCompressedData()));
        return Collections.emptySet();
    }
}
