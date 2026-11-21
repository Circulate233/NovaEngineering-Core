package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.client.memory.OwnedAnimationFrames;
import github.kasuminova.novaeng.client.memory.PixelFrameCodec;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Static sprites no longer need their CPU-side frame arrays after the atlas has
 * uploaded them. Animated sprites keep their frames for animation updates.
 */
@Mixin(TextureMap.class)
public abstract class MixinTextureMapStaticFrames {

    @Shadow
    @Final
    private Map<String, TextureAtlasSprite> mapRegisteredSprites;

    @WrapOperation(method = "loadTextureAtlas", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getFrameTextureData(I)[[I"), require = 0)
    private int[][] nova$borrowInitialUpload(final TextureAtlasSprite sprite, final int frame,
                                            final Operation<int[][]> original) {
        if (sprite.getClass() == TextureAtlasSprite.class && sprite.framesTextureData instanceof OwnedAnimationFrames owned) {
            return owned.internalView().get(frame);
        }
        return original.call(sprite, frame);
    }

    @Inject(method = "loadTextureAtlas", at = @At("RETURN"), require = 1)
    private void nova$dropStaticSpriteFrames(final IResourceManager resourceManager, final CallbackInfo callbackInfo) {
        PixelFrameCodec.Encoder encoder = null;
        for (final TextureAtlasSprite sprite : this.mapRegisteredSprites.values()) {
            if (!sprite.hasAnimationMetadata()) {
                sprite.clearFramesTextureData();
            } else if (sprite.framesTextureData instanceof OwnedAnimationFrames owned) {
                if (encoder == null) encoder = new PixelFrameCodec.Encoder();
                owned.pack(encoder);
            }
        }
    }

}
