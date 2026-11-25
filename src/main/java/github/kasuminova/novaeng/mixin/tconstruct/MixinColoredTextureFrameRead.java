package github.kasuminova.novaeng.mixin.tconstruct;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.client.memory.SpriteFrameReads;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import slimeknights.tconstruct.library.client.texture.AbstractColoredTexture;

@Mixin(value = AbstractColoredTexture.class, remap = false)
public abstract class MixinColoredTextureFrameRead {
    @WrapOperation(method = "load", at = @At(value = "INVOKE", remap = true,
        target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getFrameTextureData(I)[[I"),
        slice = @Slice(to = @At(value = "INVOKE", target = "Ljava/util/Arrays;copyOf([II)[I")), require = 0)
    private int[][] nova$readBeforePixelCopy(final TextureAtlasSprite sprite, final int frame, final Operation<int[][]> original) {
        return SpriteFrameReads.forImmediateCopy(sprite, frame, original);
    }
}
