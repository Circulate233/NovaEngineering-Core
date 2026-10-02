package github.kasuminova.novaeng.mixin.journeymap;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import journeymap.common.chunk.JMChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.ByteArrayOutputStream;

/**
 * Writes JourneyMap's chunk snapshots through one buffer per thread instead of a new one per chunk.
 *
 * <p>Every chunk is serialised into a fresh {@code ByteArrayOutputStream} that starts at 8 KB and doubles when it
 * fills. A serialised chunk is made of the block state arrays of sixteen sections plus the biome, lighting and
 * entity data, so it clears 8 KB several times over and the buffer is reallocated and copied on each doubling - that
 * buffer, together with the stream itself, was the largest single source of allocation on JourneyMap's write thread,
 * about nine tenths of everything the thread allocated.</p>
 *
 * <p>The stream never leaves the method: the bytes are copied out with {@code toByteArray()} before it returns, and
 * the {@code close()} the writer performs on its {@code DataOutputStream} reaches a {@code ByteArrayOutputStream},
 * whose {@code close()} does nothing. So the same stream can serve the next write after a {@code reset()}, which keeps
 * the array it has already grown to - sized to a whole chunk up front, so it rarely has to grow at all - and only sets
 * its length back to zero. The writes happen on one thread, but a thread local is used anyway so that a second writer
 * cannot see a half-consumed buffer.</p>
 */
@Mixin(value = JMChunkStorage.class, remap = false)
public abstract class MixinJMChunkStorage {

    @Unique
    private static final int NOVA$BUFFER_SIZE = 64 * 1024;

    @Unique
    private static final ThreadLocal<ByteArrayOutputStream> NOVA$WRITE_BUFFER = new ThreadLocal<>();

    @Redirect(
        method = "write(Ljourneymap/common/chunk/JMChunkSnapshot;)V",
        at = @At(value = "NEW", target = "(I)Ljava/io/ByteArrayOutputStream;"),
        remap = false,
        require = 1
    )
    private ByteArrayOutputStream nova$sizedWriteBuffer(final int requestedSize) {
        if (!NovaEngCoreConfig.CLIENT.optimizeJourneymapChunkBuffer) {
            return new ByteArrayOutputStream(requestedSize);
        }
        ByteArrayOutputStream buffer = NOVA$WRITE_BUFFER.get();
        if (buffer == null) {
            buffer = new ByteArrayOutputStream(NOVA$BUFFER_SIZE);
            NOVA$WRITE_BUFFER.set(buffer);
        }
        buffer.reset();
        return buffer;
    }
}
