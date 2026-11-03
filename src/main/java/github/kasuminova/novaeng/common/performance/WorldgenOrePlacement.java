package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Generation notification policy with the original overload and flags available through PRESERVE.
 */
public final class WorldgenOrePlacement {
    private WorldgenOrePlacement() {
    }

    public static boolean setDefault(final World world, final BlockPos pos, final IBlockState state) {
        if (NovaEngCoreConfig.SERVER.worldgenOreNotifications == WorldgenNotificationMode.PRESERVE
            || world.isRemote || world.captureBlockSnapshots) return world.setBlockState(pos, state);
        final int flags = flags(world, pos, state, 3);
        return flags == 3 ? world.setBlockState(pos, state) : world.setBlockState(pos, state, flags);
    }

    public static boolean set(final World world, final BlockPos pos, final IBlockState state, final int flags) {
        return world.setBlockState(pos, state, flags(world, pos, state, flags));
    }

    private static int flags(final World world, final BlockPos pos, final IBlockState state, final int original) {
        final WorldgenNotificationMode mode = NovaEngCoreConfig.SERVER.worldgenOreNotifications;
        if (mode == WorldgenNotificationMode.PRESERVE || world.isRemote || world.captureBlockSnapshots) return original;
        final int modified = mode == WorldgenNotificationMode.SILENT_ROCK ? (original & ~1) | 16 : original & ~1;
        if (modified == original || !isRock(state) || state.getBlock().hasTileEntity(state)) return original;
        final IBlockState old = world.getBlockState(pos);
        return isRock(old) && !old.getBlock().hasTileEntity(old) ? modified : original;
    }

    private static boolean isRock(final IBlockState state) {
        final Material material = state.getMaterial();
        return material == Material.ROCK || material == Material.IRON;
    }
}
