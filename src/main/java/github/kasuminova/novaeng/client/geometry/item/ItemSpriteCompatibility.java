package github.kasuminova.novaeng.client.geometry.item;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;

/**
 * Only accepts sprite subclasses that inherit the geometry readers from TextureAtlasSprite.
 */
public final class ItemSpriteCompatibility {
    private static final Method[] GEOMETRY_READS = {
        ObfuscationReflectionHelper.findMethod(TextureAtlasSprite.class, "func_94211_a", int.class),
        ObfuscationReflectionHelper.findMethod(TextureAtlasSprite.class, "func_94216_b", int.class),
        ObfuscationReflectionHelper.findMethod(TextureAtlasSprite.class, "func_110970_k", int.class),
        ObfuscationReflectionHelper.findMethod(TextureAtlasSprite.class, "func_147965_a", int[][].class, int.class),
        ObfuscationReflectionHelper.findMethod(TextureAtlasSprite.class, "func_94209_e", float.class),
        ObfuscationReflectionHelper.findMethod(TextureAtlasSprite.class, "func_94212_f", float.class),
        ObfuscationReflectionHelper.findMethod(TextureAtlasSprite.class, "func_94206_g", float.class),
        ObfuscationReflectionHelper.findMethod(TextureAtlasSprite.class, "func_94210_h", float.class),
        ObfuscationReflectionHelper.findMethod(TextureAtlasSprite.class, "func_94214_a", float.class, double.class),
        ObfuscationReflectionHelper.findMethod(TextureAtlasSprite.class, "func_94207_b", float.class, double.class)
    };
    private static final ClassValue<Boolean> VANILLA_READS = new ClassValue<>() {
        @Override
        protected Boolean computeValue(final @NonNull Class<?> type) {
            try {
                for (final Method read : GEOMETRY_READS) {
                    if (type.getMethod(read.getName(), read.getParameterTypes()).getDeclaringClass() != TextureAtlasSprite.class)
                        return false;
                }
                return true;
            } catch (NoSuchMethodException | LinkageError | SecurityException unavailable) {
                return false;
            }
        }
    };

    private ItemSpriteCompatibility() {
    }

    public static boolean hasVanillaReads(final TextureAtlasSprite sprite) {
        return sprite.getClass() == TextureAtlasSprite.class || VANILLA_READS.get(sprite.getClass());
    }
}
