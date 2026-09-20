package github.kasuminova.novaeng.mixin.actinium;

import github.kasuminova.novaeng.client.entity.LoadedEntityChunkIndexProvider;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import com.dhj.actinium.render.entity.EntityGatherer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces Actinium's all-loaded-chunk source with the owning world's entity-bearing chunk index.
 */
@Mixin(value = EntityGatherer.class, remap = false)
public abstract class MixinEntityGatherer {

    @Redirect(method = "getLoadedEntityList",
        at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;values()Lit/unimi/dsi/fastutil/objects/ObjectCollection;",
            remap = false),
        remap = false,
        require = 1
    )
    private ObjectCollection<Chunk> nova$getEntityBearingChunks(final Long2ObjectMap<Chunk> loadedChunks,
                                                                 final WorldClient world) {
        if (world instanceof LoadedEntityChunkIndexProvider provider) {
            return provider.nova$getLoadedEntityChunkIndex().values();
        }
        return loadedChunks.values();
    }
}
