package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.client.model.TextureLocationCache;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// VanillaModelWrapper is a private inner class of ModelLoader.
@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper", remap = false)
public abstract class MixinVanillaModelTextureLocations {

    @Redirect(method = "bakeNormal", at = @At(value = "NEW",
        target = "(Ljava/lang/String;)Lnet/minecraft/util/ResourceLocation;"), require = 2)
    private ResourceLocation nova$reuseTextureLocation(final String resourceName) {
        // Only reuse the immutable identifier; every face still invokes its original texture getter.
        return TextureLocationCache.parse(resourceName);
    }
}
