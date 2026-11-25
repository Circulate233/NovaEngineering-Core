package github.kasuminova.novaeng.mixin.geometry.ctm;

import github.kasuminova.novaeng.client.geometry.block.CtmMaterialProbe;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import team.chisel.ctm.client.model.ModelCTM;

import java.util.function.Function;

@Mixin(value = ModelCTM.class, remap = false, priority = 900)
public abstract class MixinModelCTM {
    /** The callback also reaches vanilla leaves below multipart and weighted parent models. */
    @ModifyArg(method = "bake", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/client/model/IModel;bake(Lnet/minecraftforge/common/model/IModelState;Lnet/minecraft/client/renderer/vertex/VertexFormat;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/block/model/IBakedModel;"), index = 2, require = 1)
    private Function<ResourceLocation, TextureAtlasSprite> nova$identifyTextureProbe(final Function<ResourceLocation, TextureAtlasSprite> getter) {
        CtmMaterialProbe.identifyGetter(this, getter);
        return getter;
    }
}
