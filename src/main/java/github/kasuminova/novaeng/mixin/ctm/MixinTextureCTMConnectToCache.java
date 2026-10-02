package github.kasuminova.novaeng.mixin.ctm;

import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import team.chisel.ctm.client.texture.render.TextureCTM;
import team.chisel.ctm.client.util.CTMLogic;
import team.chisel.ctm.client.util.IdentityStrategy;

import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Answers {@code TextureCTM.connectTo} from a reference keyed index instead of allocating a lookup key per call.
 * <p>
 * The original allocates a {@code CacheKey(from, dir)} on every invocation, cache hits included, and {@code connectTo}
 * runs once per neighbouring face while the renderer assembles a chunk's connection map. That was the largest single
 * allocation point in the client (about 11% of everything allocated, reached from
 * {@code TextureContextCTM.<init>}). The keys are also highly repetitive: {@code from} and {@code to} are block state
 * instances, which Minecraft interns per block variant, and {@code dir} has six values, so the same few combinations
 * are rebuilt over and over.
 * <p>
 * The key carries nothing beyond {@code (from, dir)}, and the original compares both halves by identity --
 * {@code CacheKey.hashCode} seeds with {@code System.identityHashCode(from)} and {@code equals} uses {@code ==}. The
 * lookup is therefore spelled directly as a reference keyed map of per-facing maps, which needs no key object and
 * reproduces the original's semantics. The innermost map keys on {@code to} by reference too, again as the original
 * did; value equality would merge states the original kept apart. The innermost maps are built with CTM's own
 * {@link IdentityStrategy} so their semantics cannot drift from the ones the original created.
 * <p>
 * There is deliberately no runtime switch here. {@code CacheKey} is package private and final, so its allocation
 * cannot be intercepted from outside CTM's package, and the original method cannot be called from an overwrite. The
 * mixin is therefore gated at apply time by {@code MixinDecisions}, the way the other overwrites in this codebase are,
 * rather than pretending to have a fallback path at runtime.
 */
@Mixin(value = TextureCTM.class, remap = false)
public abstract class MixinTextureCTMConnectToCache {

    @Shadow
    @Final
    private BiPredicate<EnumFacing, IBlockState> connectionChecks;

    @Shadow
    @Final
    private Map<?, ?> connectionCache;

    @Unique
    private Reference2ObjectOpenHashMap<IBlockState, Object2ByteMap<IBlockState>[]> nova$connectionsByDirection;

    @Overwrite(remap = false)
    public boolean connectTo(final CTMLogic logic, final IBlockState from, final IBlockState to,
                             final EnumFacing dir) {
        // The whole lookup has to sit inside this lock, exactly as the original did: a chunk's connection map is built
        // from several worker threads at once, and they all reach the same per facing map for a repeated state. The
        // original held the lock across the get and the put, so a second thread could not resize a map while the first
        // was inserting into it; releasing it after the lookup and writing unlocked made that map corrupt in its own
        // rehash (an index past the end of a half rebuilt table).
        synchronized (this.connectionCache) {
            final Object2ByteMap<IBlockState> connections = nova$connectionsFor(from, dir);
            byte cached = connections.getByte(to);
            if (cached == -1) {
                cached = (byte) (nova$connects(logic, from, to, dir) ? 1 : 0);
                connections.put(to, cached);
            }
            return cached == 1;
        }
    }

    /**
     * The original synchronised on the {@code connectionCache} map instance rather than on {@code this}. That lock is
     * kept, and it is held across the cached read and the write rather than only the lookup, so concurrent chunk
     * building serialises the same way it did before; only ever called with that lock held.
     */
    @Unique
    private Object2ByteMap<IBlockState> nova$connectionsFor(final IBlockState from, final EnumFacing dir) {
        Reference2ObjectOpenHashMap<IBlockState, Object2ByteMap<IBlockState>[]> byDirection =
                this.nova$connectionsByDirection;
        if (byDirection == null) {
            byDirection = new Reference2ObjectOpenHashMap<>();
            this.nova$connectionsByDirection = byDirection;
        }
        Object2ByteMap<IBlockState>[] perFacing = byDirection.get(from);
        if (perFacing == null) {
            perFacing = nova$newFacingArray();
            byDirection.put(from, perFacing);
        }
        Object2ByteMap<IBlockState> connections = perFacing[dir.ordinal()];
        if (connections == null) {
            connections = new Object2ByteOpenCustomHashMap<>(new IdentityStrategy<>());
            connections.defaultReturnValue((byte) -1);
            perFacing[dir.ordinal()] = connections;
        }
        return connections;
    }

    @SuppressWarnings("unchecked")
    @Unique
    private static Object2ByteMap<IBlockState>[] nova$newFacingArray() {
        return new Object2ByteMap[EnumFacing.values().length];
    }

    @Unique
    private boolean nova$connects(final CTMLogic logic, final IBlockState from, final IBlockState to,
                                  final EnumFacing dir) {
        final BiPredicate<EnumFacing, IBlockState> checks = this.connectionChecks;
        return checks != null ? checks.test(dir, to)
                              : CTMLogic.StateComparisonCallback.DEFAULT.connects(logic, from, to, dir);
    }
}
