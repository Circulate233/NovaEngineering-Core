package github.kasuminova.novaeng.mixin.actinium;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormatElement;
import com.gtnewhorizons.angelica.glsm.CompatShaderTransformer;
import com.gtnewhorizons.angelica.glsm.backend.BackendManager;
import github.kasuminova.novaeng.common.performance.PersistentTextCache;
import net.minecraft.client.Minecraft;
import org.antlr.v4.runtime.Parser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.taumc.glsl.ShaderParser;

@Mixin(value = CompatShaderTransformer.class, remap = false)
public abstract class MixinShaderTextCache {
    @Unique
    private static volatile PersistentTextCache nova$textCache;

    @Shadow
    private static String transformInternal(final String source, final boolean isFragment) {
        throw new AssertionError();
    }

    @Redirect(method = "transform", at = @At(value = "INVOKE",
        target = "Lcom/gtnewhorizons/angelica/glsm/CompatShaderTransformer;transformInternal(Ljava/lang/String;Z)Ljava/lang/String;"), require = 1)
    private static String nova$cachedTransform(final String source, final boolean isFragment) {
        PersistentTextCache cache = nova$textCache;
        if (cache == null) {
            synchronized (CompatShaderTransformer.class) {
                cache = nova$textCache;
                if (cache == null) {
                    cache = new PersistentTextCache(Minecraft.getMinecraft().gameDir.toPath()
                                                                                    .resolve("cache/novaeng/shader-text"), PersistentTextCache.implementationFingerprint(
                        CompatShaderTransformer.class, ShaderParser.class, Parser.class,
                        VertexFormatElement.class, PersistentTextCache.class));
                    nova$textCache = cache;
                }
            }
        }
        final var backend = BackendManager.RENDER_BACKEND;
        final String environment = isFragment + "/" + (backend == null ? "none/330"
            : backend.getClass().getName() + "/" + backend.getMinGLSLVersion());
        return cache.get(source, environment, () -> transformInternal(source, isFragment));
    }
}
