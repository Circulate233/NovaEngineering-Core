package github.kasuminova.novaeng.mixin.ctm;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.kasuminova.novaeng.client.ctm.CtmBakeTraceIndexImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import team.chisel.ctm.client.model.ModelCTM;

import java.util.function.Function;

/** Records the exact ResourceLocation sequence entering ModelCTM's private bake texture lambda. */
@Mixin(value = ModelCTM.class, remap = false)
public abstract class MixinModelCTMBakeTrace {

    @WrapMethod(method = "lambda$bake$3(Ljava/util/function/Function;Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;",
        remap = false, require = 1)
    private TextureAtlasSprite nova$recordTextureAccess(
        final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
        final ResourceLocation rl,
        final Operation<TextureAtlasSprite> original
    ) {
        CtmBakeTraceIndexImpl.instance().recordTextureAccess((ModelCTM) (Object) this, rl);
        return original.call(bakedTextureGetter, rl);
    }
}
