package github.kasuminova.novaeng.mixin.geometry.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import github.kasuminova.novaeng.client.geometry.block.CompactBlockModel;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper", remap = false)
public abstract class MixinVanillaModelWrapper {
    @ModifyExpressionValue(method = "bakeNormal", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/block/model/SimpleBakedModel$Builder;makeBakedModel()Lnet/minecraft/client/renderer/block/model/IBakedModel;", remap = true), require = 1)
    private IBakedModel novaeng$compactOwnedGeometry(final IBakedModel original) {
        return CompactBlockModel.fromVanillaBake((SimpleBakedModel) original);
    }
}
