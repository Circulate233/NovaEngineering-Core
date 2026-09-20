package github.kasuminova.novaeng.client.entity;

/**
 * Exposes the entity-bearing chunk index owned by one client world instance.
 */
public interface LoadedEntityChunkIndexProvider {

    /**
     * Returns the index whose lifetime is bounded by this world object.
     *
     * @return per-world loaded entity chunk index
     */
    LoadedEntityChunkIndex nova$getLoadedEntityChunkIndex();
}
