package github.kasuminova.novaeng.client.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import java.util.Objects;

/**
 * Per-world packed-coordinate implementation of {@link LoadedEntityChunkIndex}.
 */
public final class LoadedEntityChunkIndexImpl implements LoadedEntityChunkIndex {

    private final Long2ObjectOpenHashMap<Chunk> chunks = new Long2ObjectOpenHashMap<>();

    @Override
    public void put(final Chunk chunk) {
        final Chunk checkedChunk = Objects.requireNonNull(chunk, "chunk");
        this.chunks.put(ChunkPos.asLong(checkedChunk.x, checkedChunk.z), checkedChunk);
    }

    @Override
    public void remove(final int chunkX, final int chunkZ) {
        this.chunks.remove(ChunkPos.asLong(chunkX, chunkZ));
    }

    @Override
    public ObjectCollection<Chunk> values() {
        return this.chunks.values();
    }

    @Override
    public void clear() {
        this.chunks.clear();
    }
}
