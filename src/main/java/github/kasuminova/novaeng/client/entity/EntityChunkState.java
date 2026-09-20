package github.kasuminova.novaeng.client.entity;

/**
 * Exposes Minecraft's entity-presence state without reflection or an Actinium accessor dependency.
 */
public interface EntityChunkState {

    /**
     * Returns whether the chunk has contained entities since it was loaded.
     *
     * @return vanilla {@code hasEntities} state
     */
    boolean nova$hasEntities();
}
