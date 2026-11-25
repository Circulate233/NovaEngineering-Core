package github.kasuminova.novaeng.mixin.jei;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.client.memory.SpriteFrameReads;
import mezz.jei.color.ColorGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value = ColorGetter.class, remap = false)
public abstract class MixinColorGetterFrameRead {
    @WrapOperation(method = "getBufferedImage", at = @At(value = "INVOKE", remap = true,
        target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getFrameTextureData(I)[[I"),
        slice = @Slice(to = @At(value = "INVOKE", target = "Ljava/awt/image/BufferedImage;setRGB(IIII[III)V")), require = 0)
    private static int[][] nova$readBeforeImageCopy(final TextureAtlasSprite sprite, final int frame, final Operation<int[][]> original) {
        return SpriteFrameReads.forImmediateCopy(sprite, frame, original);
    }
}
