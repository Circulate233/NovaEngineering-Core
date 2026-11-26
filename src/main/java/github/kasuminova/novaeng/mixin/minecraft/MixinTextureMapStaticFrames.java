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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
    public Map<String, TextureAtlasSprite> mapRegisteredSprites;

    /** Cleanroom performs the initial upload synchronously in this Forge-named method. */
    @WrapOperation(method = "finishLoading", remap = false, require = 1, at = @At(value = "INVOKE", remap = true,
        target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;getFrameTextureData(I)[[I"))
    private int[][] nova$borrowFinishedUpload(final TextureAtlasSprite sprite, final int frame,
                                             final Operation<int[][]> original) {
        return nova$initialUploadPixels(sprite, frame, original);
    }

    @Unique
    private static int[][] nova$initialUploadPixels(final TextureAtlasSprite sprite, final int frame,
                                                   final Operation<int[][]> original) {
        if (sprite.getClass() == TextureAtlasSprite.class && sprite.framesTextureData instanceof OwnedAnimationFrames owned) {
            return owned.internalView().get(frame);
        }
        return original.call(sprite, frame);
    }

    @Redirect(method = "updateAnimations", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;updateAnimation()V"), require = 1)
    private void nova$consumePrivateAnimationFrames(final TextureAtlasSprite sprite) {
        if (sprite.framesTextureData instanceof OwnedAnimationFrames owned && !owned.isEmpty()) {
            synchronized (owned.readLock()) {
                sprite.updateAnimation();
            }
        } else {
            sprite.updateAnimation();
        }
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
