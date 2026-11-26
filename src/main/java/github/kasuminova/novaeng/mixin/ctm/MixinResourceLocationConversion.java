package github.kasuminova.novaeng.mixin.ctm;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.kasuminova.novaeng.client.model.TextureLocationCache;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import team.chisel.ctm.client.util.ResourceUtil;

@Mixin(value = ResourceUtil.class, remap = false)
public abstract class MixinResourceLocationConversion {
    @WrapMethod(method = "spriteToAbsolute", require = 1)
    private static ResourceLocation nova$reuseAbsoluteTexture(final ResourceLocation sprite,
                                                              final Operation<ResourceLocation> original) {
        if (sprite != null && sprite.getClass() == ResourceLocation.class) {
            return TextureLocationCache.absoluteTexture(sprite);
        }
        return original.call(sprite);
    }
}
