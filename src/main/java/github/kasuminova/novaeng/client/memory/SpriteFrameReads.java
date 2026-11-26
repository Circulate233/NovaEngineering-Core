package github.kasuminova.novaeng.client.memory;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public final class SpriteFrameReads {
    private SpriteFrameReads() {
    }

    /**
     * For synchronous callers that immediately copy the returned pixels before calling back into the game.
     */
    public static int[][] forImmediateCopy(final TextureAtlasSprite sprite, final int frame, final Operation<int[][]> original) {
        if (sprite.getClass() == TextureAtlasSprite.class && sprite.framesTextureData instanceof OwnedAnimationFrames owned) {
            return owned.forImmediateCopy(frame);
        }
        return original.call(sprite, frame);
    }
}
