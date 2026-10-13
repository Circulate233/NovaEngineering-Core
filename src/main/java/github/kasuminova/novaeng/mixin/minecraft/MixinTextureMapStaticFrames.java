package github.kasuminova.novaeng.mixin.minecraft;

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

    @Inject(method = "loadTextureAtlas", at = @At("RETURN"), require = 1)
    private void nova$dropStaticSpriteFrames(final IResourceManager resourceManager, final CallbackInfo callbackInfo) {
        for (final TextureAtlasSprite sprite : this.mapRegisteredSprites.values()) {
            if (!sprite.hasAnimationMetadata()) {
                sprite.clearFramesTextureData();
            }
        }
    }

}
