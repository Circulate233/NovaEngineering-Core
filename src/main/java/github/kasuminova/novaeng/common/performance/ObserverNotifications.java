package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.mixin.minecraft.AccessorObserverPalette;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.init.Blocks;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.BlockStatePaletteHashMap;
import net.minecraft.world.chunk.BlockStatePaletteLinear;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IBlockStatePalette;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public final class ObserverNotifications {
    private ObserverNotifications() {
    }

    public static void notify(final World world, final BlockPos pos, final Block changedBlock,
                              final BlockPos changedBlockPos) {
        if (world.isRemote) return;
        final IBlockState state;
        final Class<?> worldType = world.getClass();
        if ((worldType == WorldServer.class || worldType == WorldServerMulti.class)
            && world.getWorldType() != WorldType.DEBUG_ALL_BLOCK_STATES) {
            if (world.isOutsideBuildHeight(pos)) {
                state = Blocks.AIR.getDefaultState();
                if (ObserverBlockMethods.isEmpty(state)) return;
            } else {
                // Keep residency/creation and unload cancellation before considering the summary.
                final Chunk chunk = world.getChunk(pos);
                if (chunk.getClass() == Chunk.class) {
                    final ExtendedBlockStorage section = chunk.getBlockStorageArray()[pos.getY() >> 4];
                    if (section == Chunk.NULL_BLOCK_STORAGE) {
                        if (ObserverBlockMethods.isEmpty(Blocks.AIR.getDefaultState())) return;
                    } else if (section.getClass() == ExtendedBlockStorage.class) {
                        final IBlockStatePalette palette = ((AccessorObserverPalette) section.getData()).nova$getObserverPalette();
                        if ((palette.getClass() == BlockStatePaletteLinear.class || palette.getClass() == BlockStatePaletteHashMap.class)
                            && palette instanceof ObserverPalette summary && !summary.nova$mayHaveObserver()) return;
                    }
                }
                state = chunk.getBlockState(pos);
            }
        } else {
            state = world.getBlockState(pos);
        }
        try {
            state.getBlock().observedNeighborChange(state, world, pos, changedBlock, changedBlockPos);
        } catch (final Throwable failure) {
            final CrashReport report = CrashReport.makeCrashReport(failure, "Exception while updating neighbours");
            final CrashReportCategory category = report.makeCategory("Block being updated");
            category.addDetail("Source block type", () -> {
                try {
                    return String.format("ID #%d (%s // %s // %s)", Block.getIdFromBlock(changedBlock),
                        changedBlock.getTranslationKey(), changedBlock.getClass().getName(), changedBlock.getRegistryName());
                } catch (final Throwable ignored) {
                    return "ID #" + Block.getIdFromBlock(changedBlock);
                }
            });
            CrashReportCategory.addBlockInfo(category, pos, state);
            throw new ReportedException(report);
        }
    }
}
