package github.kasuminova.novaeng.mixin.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import team.chisel.ctm.client.model.ModelCTM;

import java.util.function.Function;

/** Invokes ModelCTM's private texture-initialization lambda for trace replay. */
@Mixin(value = ModelCTM.class, remap = false)
public interface InvokerModelCTMReplay {

    /** Replays one captured texture access through the original initialization logic. */
    @Invoker("lambda$bake$3")
    TextureAtlasSprite nova$replayTextureAccess(
        Function<ResourceLocation, TextureAtlasSprite> textureGetter,
        ResourceLocation location);
}
