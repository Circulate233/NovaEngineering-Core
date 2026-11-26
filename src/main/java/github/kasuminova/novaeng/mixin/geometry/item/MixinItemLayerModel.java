package github.kasuminova.novaeng.mixin.geometry.item;

import com.google.common.collect.ImmutableList;
import github.kasuminova.novaeng.client.geometry.GeometryGeneration;
import github.kasuminova.novaeng.client.geometry.item.CompactItemModel;
import github.kasuminova.novaeng.client.geometry.item.ItemContour;
import github.kasuminova.novaeng.client.geometry.item.ItemSpriteCompatibility;
import github.kasuminova.novaeng.client.memory.OwnedAnimationFrames;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.Attributes;
import net.minecraftforge.client.model.BakedItemModel;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;
import java.util.function.Function;

@Mixin(value = ItemLayerModel.class, remap = false)
public abstract class MixinItemLayerModel {
    // The shadow must retain ItemLayerModel's exact Guava field descriptor.
    @Shadow
    @Final
    private ImmutableList<ResourceLocation> textures;
    @Shadow
    @Final
    private ItemOverrideList overrides;

    /**
     * @author NovaEngineering
     * @reason Bake generated items directly from compact contours.
     */
    @Overwrite
    public IBakedModel bake(final IModelState state, final VertexFormat format,
                            final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        final Optional<TRSRTransformation> transform = state.apply(Optional.empty());
        final boolean identity = transform.isEmpty() || transform.get().isIdentity();
        if (!identity || !format.equals(DefaultVertexFormats.ITEM) && !format.equals(Attributes.DEFAULT_BAKED_FORMAT))
            return novaeng$vanillaItem(state, format, bakedTextureGetter, transform, null, 0, identity);
        final ItemContour[] contours = new ItemContour[textures.size()];
        final TextureAtlasSprite[] sprites = new TextureAtlasSprite[textures.size()];
        for (int layer = 0; layer < textures.size(); layer++) {
            final TextureAtlasSprite sprite = bakedTextureGetter.apply(textures.get(layer));
            sprites[layer] = sprite;
            ItemContour contour = null;
            if (sprite != null && ItemSpriteCompatibility.hasVanillaReads(sprite)
                && sprite.getIconWidth() > 0 && sprite.getIconWidth() <= 256
                && sprite.getIconHeight() > 0 && sprite.getIconHeight() <= 256) {
                if (sprite.framesTextureData instanceof OwnedAnimationFrames owned) {
                    if (sprite.getFrameCount() == 0) {
                        contour = ItemContour.createFromMipmaps(sprite.getIconWidth(), sprite.getIconHeight(),
                            0, owned);
                    } else synchronized (owned.readLock()) {
                        contour = ItemContour.createFromMipmaps(sprite.getIconWidth(), sprite.getIconHeight(),
                            sprite.getFrameCount(), owned.internalView());
                    }
                } else {
                    final int[][] frames = new int[sprite.getFrameCount()][];
                    for (int frame = 0; frame < frames.length; frame++) {
                        final int[][] mip = sprite.getFrameTextureData(frame);
                        frames[frame] = mip == null || mip.length == 0 ? null : mip[0];
                    }
                    contour = ItemContour.create(sprite.getIconWidth(), sprite.getIconHeight(), frames);
                }
            }
            if (contour == null) {
                return novaeng$vanillaItem(state, format, bakedTextureGetter, transform, sprites, layer + 1, true);
            }
            contours[layer] = GeometryGeneration.intern(contour);
        }
        final TextureAtlasSprite particle = bakedTextureGetter.apply(textures.isEmpty() ? new ResourceLocation("missingno") : textures.getFirst());
        return CompactItemModel.fromOwnedLayers(contours, sprites, particle, PerspectiveMapWrapper.getTransforms(state), overrides, format);
    }

    @Unique
    // Forge's getQuadsForSprite API requires this Optional; reuse the value already read during bake.
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private IBakedModel novaeng$vanillaItem(final IModelState state, final VertexFormat format,
                                            final Function<ResourceLocation, TextureAtlasSprite> getter,
                                            final Optional<TRSRTransformation> transform,
                                            final TextureAtlasSprite[] observed, final int observedCount,
                                            final boolean identity) {
        // Forge's original bake, using only the state/texture reads already consumed by the fast path.
        final ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();
        for (int layer = 0; layer < textures.size(); layer++) {
            final TextureAtlasSprite sprite = layer < observedCount ? observed[layer] : getter.apply(textures.get(layer));
            quads.addAll(ItemLayerModel.getQuadsForSprite(layer, sprite, format, transform));
        }
        final TextureAtlasSprite particle = getter.apply(textures.isEmpty() ? new ResourceLocation("missingno") : textures.getFirst());
        return new BakedItemModel(quads.build(), particle, PerspectiveMapWrapper.getTransforms(state), overrides, identity);
    }
}
