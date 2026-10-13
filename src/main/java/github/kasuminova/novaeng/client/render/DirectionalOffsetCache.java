package github.kasuminova.novaeng.client.render;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public final class DirectionalOffsetCache {

    private static final ThreadLocal<DirectionalOffsetCache> MEMO =
            ThreadLocal.withInitial(DirectionalOffsetCache::new);

    private BlockPos position;
    private int offsetX;
    private int offsetY;
    private int offsetZ;
    private BlockPos result;
    private BlockPos facingPosition;
    private EnumFacing facing;
    private BlockPos facingResult;

    private DirectionalOffsetCache() {
    }

    public static DirectionalOffsetCache current() {
        return MEMO.get();
    }

    public BlockPos add(final BlockPos position, final Vec3i offset) {
        final int offsetX = offset.getX();
        final int offsetY = offset.getY();
        final int offsetZ = offset.getZ();
        if (this.position == position && this.offsetX == offsetX && this.offsetY == offsetY
            && this.offsetZ == offsetZ) {
            return this.result;
        }
        final BlockPos result = position.add(offset);
        this.position = position;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.result = result;
        return result;
    }

    public BlockPos offset(final BlockPos position, final EnumFacing facing) {
        if (this.facingPosition == position && this.facing == facing) {
            return this.facingResult;
        }
        final BlockPos result = position.offset(facing);
        this.facingPosition = position;
        this.facing = facing;
        this.facingResult = result;
        return result;
    }
}
