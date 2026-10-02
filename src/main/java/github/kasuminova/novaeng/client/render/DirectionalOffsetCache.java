package github.kasuminova.novaeng.client.render;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

/**
 * Remembers the last neighbour position a direction resolved to, so that the repeat asks that follow reuse it.
 *
 * <p>Chisel's CTM resolves the neighbouring block of a face through {@code Dir.applyConnection}, which is
 * {@code pos.add(offset)}: a fresh {@code BlockPos} for every ask. The caller that matters walks all eight
 * {@code Dir} constants of one face, and passes the same position and the same facing to each of them - the offset
 * depends only on those two, so the eight asks are eight identical computations, seven of which can answer from the
 * previous result.</p>
 *
 * <p>The memo holds one entry per thread, which is exactly the lifetime the repeat asks have: they happen back to
 * back inside a single {@code buildConnectionMap} call on one meshing thread. A hit returns the very instance the
 * miss produced, which is sound because {@code BlockPos} is immutable and the eight asks are asking for the same
 * value, not merely an equal one. The thread local keeps two threads building different sections from seeing each
 * other's entries, and a miss costs nothing beyond what the original always paid.</p>
 */
public final class DirectionalOffsetCache {

    private static final ThreadLocal<DirectionalOffsetCache> MEMO =
            ThreadLocal.withInitial(DirectionalOffsetCache::new);

    private BlockPos position;
    private Vec3i offset;
    private BlockPos result;

    private DirectionalOffsetCache() {
    }

    /**
     * @return the memo belonging to the calling thread; never {@code null}
     */
    public static DirectionalOffsetCache current() {
        return MEMO.get();
    }

    /**
     * Answers {@code position.add(offset)}, from the memo when it is the same ask as last time.
     */
    public BlockPos add(final BlockPos position, final Vec3i offset) {
        if (this.position == position && this.offset == offset) {
            return this.result;
        }
        final BlockPos result = position.add(offset);
        this.position = position;
        this.offset = offset;
        this.result = result;
        return result;
    }
}
