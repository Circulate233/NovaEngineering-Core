package github.kasuminova.novaeng.client.entity;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.minecraft.world.chunk.Chunk;

/**
 * Tracks client chunks whose entity sections may need to be inspected by the renderer.
 *
 * <p>The index deliberately follows vanilla's monotonic {@code Chunk.hasEntities} behavior: a
 * chunk remains indexed after its last entity leaves and is removed only when the chunk unloads.</p>
 */
public interface LoadedEntityChunkIndex {

    /**
     * Adds or replaces the chunk at its packed coordinate key.
     *
     * @param chunk loaded client chunk which has contained an entity
     */
    void put(Chunk chunk);

    /**
     * Removes an unloaded chunk coordinate.
     *
     * @param chunkX chunk x coordinate
     * @param chunkZ chunk z coordinate
     */
    void remove(int chunkX, int chunkZ);

    /**
     * Returns the live collection consumed by Actinium's existing entity gather loop.
     *
     * @return indexed chunk values
     */
    ObjectCollection<Chunk> values();

    /**
     * Releases all chunk references owned by this world index.
     */
    void clear();
}
