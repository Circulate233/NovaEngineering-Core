package github.kasuminova.novaeng.client.ctm;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import team.chisel.ctm.client.model.ModelCTM;

import java.util.function.Function;

/**
 * Records and replays the texture-initialization side effects of identical simple CTM wrap bakes.
 */
public interface CtmBakeTraceIndex {

    /** Starts an independent CTM reload generation and rejects nesting. */
    void begin();

    /** Returns whether CTM trace collection is active for the current reload. */
    boolean isActive();

    /**
     * Executes the first bake for a key or replays its completed texture-access trace on a fresh
     * {@link ModelCTM} instance.
     *
     * @return the existing baked parent because TextureMetadataHandler discards ModelCTM's result
     */
    IBakedModel bakeOrReplay(ModelCTM wrapper,
                             IModel sourceModel,
                             IModelState state,
                             VertexFormat format,
                             Function<ResourceLocation, TextureAtlasSprite> textureGetter,
                             IBakedModel existingParent,
                             BakeOperation original);

    /** Records one actual invocation of ModelCTM's private bake texture lambda. */
    void recordTextureAccess(ModelCTM wrapper, ResourceLocation location);

    /** Ends the CTM generation and releases traces, keys, models, and functions. */
    void end();

    /** Supplies the original ModelCTM bake without exposing MixinExtras outside the mixin layer. */
    @FunctionalInterface
    interface BakeOperation {
        /** Executes the original bake invocation. */
        IBakedModel call();
    }
}
