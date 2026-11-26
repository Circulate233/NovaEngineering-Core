package github.kasuminova.novaeng.client.render;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public final class DirectionalOffsetCache {

    private static final ThreadLocal<DirectionalOffsetCache> MEMO =
            ThreadLocal.withInitial(DirectionalOffsetCache::new);

    // CTM alternates positions and directions. Intern the resulting coordinate, rather than
    // keeping only the previous input identity. Never retain a caller's mutable position.
    private static final int CAPACITY = 512;
    private final BlockPos[] results = new BlockPos[CAPACITY];

    DirectionalOffsetCache() {
    }

    public static DirectionalOffsetCache current() {
        return MEMO.get();
    }

    public BlockPos add(final BlockPos position, final Vec3i offset) {
        final int offsetX = offset.getX();
        final int offsetY = offset.getY();
        final int offsetZ = offset.getZ();
        // Preserve vanilla's zero-offset alias/copy behavior and arbitrary subclass overrides.
        if ((offsetX | offsetY | offsetZ) == 0 || !isVanillaPosition(position)) return position.add(offset);
        final int x = position.getX() + offsetX;
        final int y = position.getY() + offsetY;
        final int z = position.getZ() + offsetZ;
        final int slot = slot(x, y, z);
        final BlockPos cached = results[slot];
        if (matches(cached, x, y, z)) return cached;
        return remember(slot, position.add(offset));
    }

    public BlockPos offset(final BlockPos position, final EnumFacing facing) {
        if (!isVanillaPosition(position)) return position.offset(facing);
        final int x = position.getX() + facing.getXOffset();
        final int y = position.getY() + facing.getYOffset();
        final int z = position.getZ() + facing.getZOffset();
        final int slot = slot(x, y, z);
        final BlockPos cached = results[slot];
        if (matches(cached, x, y, z)) return cached;
        return remember(slot, position.offset(facing));
    }

    private static boolean isVanillaPosition(final BlockPos position) {
        final Class<?> type = position.getClass();
        return type == BlockPos.class || type == BlockPos.MutableBlockPos.class
            || type == BlockPos.PooledMutableBlockPos.class;
    }

    private static int slot(final int x, final int y, final int z) {
        // Keep all 32 bits of each coordinate; BlockPos.toLong truncates large coordinates.
        int hash = x * 73428767 ^ y * 912931 ^ z * 438289;
        hash ^= hash >>> 16;
        return hash & (CAPACITY - 1);
    }

    private static boolean matches(final BlockPos cached, final int x, final int y, final int z) {
        return cached != null && cached.getX() == x && cached.getY() == y && cached.getZ() == z;
    }

    private BlockPos remember(final int slot, final BlockPos result) {
        // Only immutable vanilla results may escape to another caller through the cache.
        if (result.getClass() == BlockPos.class) results[slot] = result;
        return result;
    }
}
