package github.kasuminova.novaeng.common.performance;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

/** JourneyMap 6.0.10's section format; scratch is borrowed only for the synchronous write. */
public final class JourneyMapSectionWriter {
    private static final ThreadLocal<Scratch> IDLE = new ThreadLocal<>();
    // Weak keys preserve state identity and do not keep an old registry alive. The weight also
    // bounds unusually long mod property names rather than merely bounding the entry count.
    private static final Cache<IBlockState, Prefix> STATE_PREFIXES = CacheBuilder.newBuilder()
        .weakKeys().maximumWeight(4 * 1024 * 1024)
        .weigher((IBlockState state, Prefix prefix) -> prefix.bytes.length + 96).build();

    private JourneyMapSectionWriter() {
    }

    public static void write(final DataOutput out, final ExtendedBlockStorage section) throws IOException {
        Scratch scratch = IDLE.get();
        if (scratch == null) scratch = new Scratch();
        else IDLE.set(null);
        try {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        final IBlockState state = section.get(x, y, z);
                        int index = scratch.indices.getInt(state);
                        if (index < 0) {
                            index = scratch.palette.size();
                            scratch.palette.add(state);
                            scratch.indices.put(state, index);
                        }
                        final int offset = ((y * 256) + (z * 16) + x) * 2;
                        scratch.bytes[offset] = (byte) (index >>> 8);
                        scratch.bytes[offset + 1] = (byte) index;
                    }
                }
            }
            out.writeInt(scratch.palette.size());
            for (int index = 0; index < scratch.palette.size(); index++) out.write(prefix(scratch.palette.get(index)));
            // DataOutput writes these bytes verbatim: exactly the original big-endian writeShort loop.
            out.write(scratch.bytes);
        } finally {
            scratch.palette.clear();
            scratch.indices.clear();
            // Do not overwrite the scratch of an inner write; at most one idle scratch is retained.
            if (IDLE.get() == null) IDLE.set(scratch);
        }
    }

    private static byte[] prefix(final IBlockState state) throws IOException {
        final long epoch = JourneyMapMetadataEpoch.current();
        final ResourceLocation registryName = Block.REGISTRY.getNameForObject(state.getBlock());
        final Prefix cached = STATE_PREFIXES.getIfPresent(state);
        if (cached != null && cached.epoch == epoch && Objects.equals(cached.registryName, registryName)) {
            return cached.bytes;
        }
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream(128);
        final DataOutputStream out = new DataOutputStream(bytes);
        // Use NBTUtil itself, including modded IProperty.getName and NBT map iteration order.
        final NBTTagCompound tag = NBTUtil.writeBlockState(new NBTTagCompound(), state);
        out.writeUTF(tag.getString("Name"));
        if (tag.hasKey("Properties", 10)) {
            final NBTTagCompound properties = tag.getCompoundTag("Properties");
            final Set<String> keys = properties.getKeySet();
            out.writeByte(keys.size());
            for (final String key : keys) {
                out.writeUTF(key);
                out.writeUTF(properties.getString(key));
            }
        } else out.writeByte(0);
        final byte[] encoded = bytes.toByteArray();
        // Only registry-owned canonical listed states are immutable. Unknown/custom states
        // still receive the original NBT conversion on every use.
        for (final IBlockState canonical : state.getBlock().getBlockState().getValidStates()) {
            if (canonical == state) {
                // A registry mutation invalidates in-flight work as well as earlier entries.
                if (JourneyMapMetadataEpoch.current() == epoch) {
                    STATE_PREFIXES.put(state, new Prefix(epoch, registryName, encoded));
                }
                break;
            }
        }
        return encoded;
    }

    /** Registry names can change during a registry restore/remap; the owner must invalidate then. */
    public static void clearMetadata() {
        JourneyMapMetadataEpoch.invalidate();
        STATE_PREFIXES.invalidateAll();
    }

    private record Prefix(long epoch, ResourceLocation registryName, byte[] bytes) {
    }

    private static final class Scratch {
        private final ObjectArrayList<IBlockState> palette = new ObjectArrayList<>();
        private final Reference2IntOpenHashMap<IBlockState> indices = new Reference2IntOpenHashMap<>();
        private final byte[] bytes = new byte[8192];

        private Scratch() {
            indices.defaultReturnValue(-1);
        }
    }
}
