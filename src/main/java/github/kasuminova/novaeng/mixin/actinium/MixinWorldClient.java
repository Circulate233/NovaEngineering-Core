package github.kasuminova.novaeng.mixin.actinium;

import github.kasuminova.novaeng.client.entity.LoadedEntityChunkIndex;
import github.kasuminova.novaeng.client.entity.LoadedEntityChunkIndexImpl;
import github.kasuminova.novaeng.client.entity.LoadedEntityChunkIndexProvider;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Owns the entity chunk index on the same object that owns the client chunk provider.
 */
@Mixin(WorldClient.class)
public abstract class MixinWorldClient implements LoadedEntityChunkIndexProvider {

    @Unique
    private final LoadedEntityChunkIndex nova$loadedEntityChunkIndex = new LoadedEntityChunkIndexImpl();

    /**
     * Exposes this world's index to chunk lifecycle hooks and Actinium's gatherer.
     *
     * @return this world's index
     */
    @Override
    public LoadedEntityChunkIndex nova$getLoadedEntityChunkIndex() {
        return this.nova$loadedEntityChunkIndex;
    }
}
