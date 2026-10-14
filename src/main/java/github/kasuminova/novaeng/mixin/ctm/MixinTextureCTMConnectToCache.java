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

    /**
     * @author NovaEngineering contributors
     * @reason Cache connections by source state and direction without allocating composite lookup keys.
     */
    @Overwrite(remap = false)
    public boolean connectTo(final CTMLogic logic, final IBlockState from, final IBlockState to,
                             final EnumFacing dir) {
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

    @Unique
    private Object2ByteMap<IBlockState> nova$connectionsFor(final IBlockState from, final EnumFacing dir) {
        Reference2ObjectOpenHashMap<IBlockState, Object2ByteMap<IBlockState>[]> byDirection =
                this.nova$connectionsByDirection;
        if (byDirection == null) {
            byDirection = new Reference2ObjectOpenHashMap<>();
            this.nova$connectionsByDirection = byDirection;
        }
        Object2ByteMap<IBlockState>[] perFacing = byDirection.computeIfAbsent(from, k -> nova$newFacingArray());
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
