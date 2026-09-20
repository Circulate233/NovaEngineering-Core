package github.kasuminova.novaeng.mixin.ctm;

import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import team.chisel.ctm.api.texture.ICTMTexture;
import team.chisel.ctm.client.model.ModelCTM;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;

import java.util.Collection;
import java.util.Map;

/**
 * Provides non-reflective access to the simple-wrap invariants and lazily initialized maps.
 */
@Mixin(value = ModelCTM.class, remap = false)
public interface AccessorModelCTMReplay {

    /** Returns the optional vanilla model metadata; simple wraps require {@code null}. */
    @Accessor("modelinfo")
    ModelBlock nova$getModelInfo();

    /** Returns per-face overrides; simple wraps require this map to be empty. */
    @Accessor("overrides")
    Int2ObjectMap<JsonElement> nova$getOverrides();

    /** Returns parsed CTM metadata overrides; simple wraps require this map to be empty. */
    @Accessor("metaOverrides")
    Int2ObjectMap<IMetadataSectionCTM> nova$getMetaOverrides();

    /** Returns the wrapped vanilla model; simple wraps require the outer source identity. */
    @Accessor("vanillamodel")
    IModel nova$getVanillaModel();

    /** Returns UV-lock override state; simple wraps require the unset {@code null} state. */
    @Accessor("uvlock")
    Boolean nova$getUvLock();

    /** Returns the texture dependency values included in the replay key fingerprint. */
    @Accessor("textureDependencies")
    Collection<ResourceLocation> nova$getTextureDependencies();

    /** Returns generated CTM textures; fresh simple wrappers require this map to be empty. */
    @Accessor("textures")
    Map<String, ICTMTexture<?>> nova$getTextures();

    /** Returns the accumulated render-layer mask; fresh simple wrappers require zero. */
    @Accessor("layers")
    byte nova$getLayers();

    /** Returns the lazily initialized sprite override map. */
    @Accessor("spriteOverrides")
    Int2ObjectMap<TextureAtlasSprite> nova$getSpriteOverrides();

    /** Initializes the sprite override map for a replayed simple wrapper. */
    @Accessor("spriteOverrides")
    void nova$setSpriteOverrides(Int2ObjectMap<TextureAtlasSprite> overrides);

    /** Returns the lazily initialized CTM texture override map. */
    @Accessor("textureOverrides")
    Map<Pair<Integer, String>, ICTMTexture<?>> nova$getTextureOverrides();

    /** Initializes the CTM texture override map for a replayed simple wrapper. */
    @Accessor("textureOverrides")
    void nova$setTextureOverrides(Map<Pair<Integer, String>, ICTMTexture<?>> overrides);
}
