package github.kasuminova.novaeng.mixin.electroblobs;

import electroblob.wizardry.block.BlockBookshelf;
import electroblob.wizardry.client.model.ModelBookshelf;
import github.kasuminova.novaeng.client.model.async.ModelBookshelfBakeCache;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/** Builds each Wizardry bookshelf variant off-thread and publishes only complete results. */
@Mixin(value = ModelBookshelf.class, remap = false)
public abstract class MixinModelBookshelfConcurrent {

    @Shadow
    @Final
    private static List<ResourceLocation> bookModelLocations;

    @Final
    @Shadow
    private String variant;

    @Overwrite(remap = false)
    protected IBakedModel[][] getBakedBookModels(final VertexFormat format,
                                                  final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) throws Exception {
        final String key = this.variant == null ? "\u0000" : this.variant;
        final IBakedModel[][] cached = ModelBookshelfBakeCache.get(key);
        if (cached != null) {
            return cached;
        }

        final ImmutableList<ResourceLocation> textures = BlockBookshelf.getBookTextures();
        final IBakedModel[][] built = new IBakedModel[textures.size()][12];
        for (int i = 0; i < textures.size(); ++i) {
            final ImmutableMap<String, String> retexturer = ImmutableMap.of(
                "books", textures.get(i).toString());
            for (int j = 0; j < 12; ++j) {
                final IModel bookModel = ModelLoaderRegistry.getModel(
                    new ModelResourceLocation(bookModelLocations.get(j), this.variant))
                    .retexture(retexturer);
                built[i][j] = bookModel.bake(
                    bookModel.getDefaultState(), format, bakedTextureGetter);
            }
        }

        return ModelBookshelfBakeCache.putIfAbsent(key, built);
    }
}
