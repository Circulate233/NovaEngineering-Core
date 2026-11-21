package github.kasuminova.novaeng.mixin.journeymap;

import github.kasuminova.novaeng.common.performance.JourneyMapQuadScratch;
import journeymap.client.cartography.color.ColoredSprite;
import journeymap.client.mod.vanilla.VanillaBlockSpriteProxy;
import journeymap.common.helper.JMResourceLocation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.List;

@Mixin(value = VanillaBlockSpriteProxy.class, remap = false)
public abstract class MixinVanillaBlockSpriteDedupe {
    /**
     * @author NovaEngineering
     * @reason Deduplicate quad identities in one pass without JDK bucket-order emulation or a temporary list.
     */
    @Overwrite
    public boolean addSprites(final HashMap<String, ColoredSprite> sprites, final List<BakedQuad> quads) {
        if (quads == null || quads.isEmpty()) return false;
        if (quads.size() == 1) return nova$addSprite(sprites, quads.getFirst());
        final JourneyMapQuadScratch scratch = JourneyMapQuadScratch.begin();
        try {
            boolean added = false;
            for (final BakedQuad quad : quads) {
                if (scratch.set.add(quad)) added |= nova$addSprite(sprites, quad);
            }
            return added;
        } finally {
            scratch.close();
        }
    }

    @Unique
    private static boolean nova$addSprite(final HashMap<String, ColoredSprite> sprites, final BakedQuad quad) {
        final TextureAtlasSprite sprite = quad.getSprite();
        if (sprite == null) return false;
        final String name = sprite.getIconName();
        if (sprites.containsKey(name) || JMResourceLocation.parse(name).equals(JMResourceLocation.parse("missingno"))) return false;
        sprites.put(name, new ColoredSprite(quad));
        return true;
    }
}
