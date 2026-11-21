package github.kasuminova.novaeng.client.moreoverlays;

import at.feldim2425.moreoverlays.api.lightoverlay.LightScannerBase;
import at.feldim2425.moreoverlays.config.Config;
import at.feldim2425.moreoverlays.lightoverlay.LightScannerVanilla;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

/**
 * Reusable scan storage. Exposing the original mutable list permanently restores its clear/refill behavior.
 */
public final class LightScanCache {
    private static final AxisAlignedBB TEST_BB = new AxisAlignedBB(0.3, 0, 0.3, 0.7, 1, 0.7);
    private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
    private int[] coordinates = new int[192];
    private byte[] modes = new byte[64];
    private int size;
    private boolean exposed;

    public static boolean supports(final LightScannerBase scanner) {
        return scanner.getClass() == LightScannerVanilla.class;
    }

    public void update(final LightScannerBase scanner, final EntityPlayer player,
                       final List<Pair<BlockPos, Byte>> publicModes) {
        final int px = (int) Math.floor(player.posX);
        final int py = (int) Math.floor(player.posY);
        final int pz = (int) Math.floor(player.posZ);
        final int y1 = py - Config.light_DownRange;
        final int y2 = py + Config.light_UpRange;
        clear();
        publicModes.clear();
        for (int xo = -Config.light_HRange; xo <= Config.light_HRange; ++xo) {
            for (int zo = -Config.light_HRange; zo <= Config.light_HRange; ++zo) {
                position.setPos(px + xo, py, pz + zo);
                if (!scanner.shouldCheck(position, player.world)) continue;
                for (int y = y1; y <= y2; ++y) {
                    position.setPos(px + xo, y, pz + zo);
                    final byte mode = spawnMode(position, player.world);
                    if (mode == 0) continue;
                    if (exposed) publicModes.add(Pair.of(position.toImmutable(), mode));
                    else add(position.getX(), y, position.getZ(), mode);
                }
            }
        }
    }

    /**
     * Called only by the exact vanilla scanner's scan; extension scanners retain their original methods.
     */
    byte spawnMode(final BlockPos pos, final World world) {
        if (world.getLightFor(EnumSkyBlock.BLOCK, pos) >= Config.light_SaveLevel) return 0;
        below.setPos(pos.getX(), pos.getY() - 1, pos.getZ());
        final IBlockState state = world.getBlockState(below);
        if (!state.getBlock().canCreatureSpawn(state, world, below, EntityLiving.SpawnPlacementType.ON_GROUND))
            return 0;
        if (!checkCollision(pos, world)) return 0;
        return (byte) (world.getLightFor(EnumSkyBlock.SKY, pos) >= Config.light_SaveLevel ? 1 : 2);
    }

    private boolean checkCollision(final BlockPos pos, final World world) {
        final IBlockState state = world.getBlockState(pos);
        above.setPos(pos.getX(), pos.getY() + 1, pos.getZ());
        if (state.isNormalCube() || !Config.light_IgnoreLayer && world.getBlockState(above).isNormalCube())
            return false;
        if (world.isAirBlock(pos) && (Config.light_IgnoreLayer || world.isAirBlock(above))) return true;
        final AxisAlignedBB bb = TEST_BB.offset(pos.getX(), pos.getY(), pos.getZ());
        if (world.getCollisionBoxes(null, bb).isEmpty() && !world.containsAnyLiquid(bb)) {
            if (Config.light_IgnoreLayer) return true;
            final AxisAlignedBB bb2 = bb.offset(0, 1, 0);
            return world.getCollisionBoxes(null, bb2).isEmpty() && !world.containsAnyLiquid(bb2);
        }
        return false;
    }

    void add(final int x, final int y, final int z, final byte mode) {
        if (size == modes.length) {
            final int capacity = size + (size >> 1);
            modes = Arrays.copyOf(modes, capacity);
            coordinates = Arrays.copyOf(coordinates, capacity * 3);
        }
        final int offset = size * 3;
        coordinates[offset] = x;
        coordinates[offset + 1] = y;
        coordinates[offset + 2] = z;
        modes[size++] = mode;
    }

    public List<Pair<BlockPos, Byte>> expose(final List<Pair<BlockPos, Byte>> publicModes) {
        if (!exposed) {
            for (int i = 0; i < size; i++) publicModes.add(Pair.of(new BlockPos(x(i), y(i), z(i)), mode(i)));
            exposed = true;
            coordinates = null;
            modes = null;
            size = 0;
        }
        return publicModes;
    }

    public void clear() {
        size = 0;
    }

    public boolean isExposed() {
        return exposed;
    }

    public int size() {
        return size;
    }

    public int x(final int index) {
        return coordinates[index * 3];
    }

    public int y(final int index) {
        return coordinates[index * 3 + 1];
    }

    public int z(final int index) {
        return coordinates[index * 3 + 2];
    }

    public byte mode(final int index) {
        return modes[index];
    }
}
