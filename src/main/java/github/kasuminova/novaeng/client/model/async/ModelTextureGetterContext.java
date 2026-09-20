package github.kasuminova.novaeng.client.model.async;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

import java.util.function.Function;
import java.util.function.Supplier;

public final class ModelTextureGetterContext {

    private static final ThreadLocal<Function<ResourceLocation, TextureAtlasSprite>> CURRENT = new ThreadLocal<>();

    private ModelTextureGetterContext() {
    }

    public static Function<ResourceLocation, TextureAtlasSprite> current() {
        return CURRENT.get();
    }

    public static <T> T with(final Function<ResourceLocation, TextureAtlasSprite> getter,
                              final Supplier<T> action) {
        final Function<ResourceLocation, TextureAtlasSprite> previous = CURRENT.get();
        CURRENT.set(getter);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
