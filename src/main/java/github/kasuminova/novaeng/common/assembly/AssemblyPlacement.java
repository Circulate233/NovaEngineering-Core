package github.kasuminova.novaeng.common.assembly;

import github.kasuminova.novaeng.NovaEngineeringCore;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.Loader;


/**
 * Places through the normal block/TE hooks and settles nearby static writes together.
 */
final class AssemblyPlacement {
    private static final int BATCH_SIZE = 64;
    private static final int SILENT = Constants.BlockFlags.NO_OBSERVERS;
    private static final int NOTIFY = Constants.BlockFlags.DEFAULT;
    private static final boolean CHUNK_HOOKS_LIGHT = Loader.isModLoaded("pulsar");
    private static final ReferenceOpenHashSet<Block> STATIC_BLOCKS = ReferenceOpenHashSet.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.STONEBRICK, Blocks.BRICK_BLOCK, Blocks.PLANKS, Blocks.OBSIDIAN,
        Blocks.CONCRETE, Blocks.HARDENED_CLAY, Blocks.STAINED_HARDENED_CLAY, Blocks.IRON_BLOCK, Blocks.QUARTZ_BLOCK,
        Blocks.PRISMARINE, Blocks.GLASS, Blocks.STAINED_GLASS, Blocks.GOLD_BLOCK, Blocks.GLOWSTONE, Blocks.SEA_LANTERN);

    private final ReferenceArrayList<WorldServer> worlds = new ReferenceArrayList<>(BATCH_SIZE);
    private final ReferenceArrayList<BlockPos> positions = new ReferenceArrayList<>(BATCH_SIZE);
    private final ReferenceArrayList<Chunk> chunks = new ReferenceArrayList<>(BATCH_SIZE);
    private final ReferenceArrayList<IBlockState> previous = new ReferenceArrayList<>(BATCH_SIZE);
    private final ReferenceArrayList<IBlockState> placed = new ReferenceArrayList<>(BATCH_SIZE);
    private boolean flushing;
    private final BooleanArrayList needsLight = new BooleanArrayList(BATCH_SIZE);

    static boolean accepts(final WorldServer world) {
        return !world.captureBlockSnapshots && !world.restoringBlockSnapshots
            && world.getWorldType() != WorldType.DEBUG_ALL_BLOCK_STATES;
    }

    static boolean fluid(final IBlockState state) {
        final var block = state.getBlock();
        return block instanceof BlockLiquid || block instanceof IFluidBlock || state.getMaterial().isLiquid();
    }

    static boolean space(final EntityPlayerMP player, final WorldServer world, final BlockPos pos,
                         final IBlockState state, final boolean ignoreFluids) {
        return player.isCreative() || state.getBlock() == Blocks.AIR || state.getBlock().isAir(state, world, pos)
            || ignoreFluids && fluid(state);
    }

    static boolean authorized(final EntityPlayerMP player, final WorldServer world, final BlockPos pos, final ItemStack tool) {
        return player.world == world && player.isEntityAlive() && player.capabilities.allowEdit
            && player.canPlayerEdit(pos, EnumFacing.UP, tool) && world.isBlockModifiable(player, pos);
    }

    private static boolean stillCurrent(final WorldServer world, final Chunk chunk, final BlockPos pos,
                                        final IBlockState expected) {
        return world.getChunkProvider().getLoadedChunk(chunk.x, chunk.z) == chunk && chunk.getBlockState(pos).equals(expected);
    }

    private static boolean isStatic(final Block block) {
        return block == Blocks.AIR || STATIC_BLOCKS.contains(block);
    }

    static boolean isStatic(final IBlockState state) {
        return isStatic(state.getBlock());
    }

    private static long lightProperties(final WorldServer world, final BlockPos pos, final IBlockState state) {
        return (long) state.getLightOpacity(world, pos) << 32 | Integer.toUnsignedLong(state.getLightValue(world, pos));
    }

    private static boolean write(final WorldServer world, final Chunk chunk, final BlockPos pos,
                                 final IBlockState state, final boolean fast) {
        // Pulsar hooks Chunk.setBlockState to queue both section and block light changes.
        return fast ? chunk.setBlockState(pos, state) != null : world.setBlockState(pos, state, SILENT);
    }

    private static void restore(final BlockSnapshot snapshot, final WorldServer world, final Chunk chunk, final BlockPos pos,
                                final IBlockState state, final boolean fast, final long oldLight) {
        final boolean restoring = world.restoringBlockSnapshots;
        world.restoringBlockSnapshots = true;
        try {
            if (!write(world, chunk, pos, state, fast)) {
                NovaEngineeringCore.log.error("Could not restore cancelled construction at {}", pos);
                return;
            }
            if (snapshot.getNbt() != null) {
                final var tile = chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
                if (tile != null) {
                    tile.readFromNBT(snapshot.getNbt());
                    tile.markDirty();
                }
            }
            if (fast && !CHUNK_HOOKS_LIGHT && oldLight != lightProperties(world, pos, state)) world.checkLight(pos);
        } finally {
            world.restoringBlockSnapshots = restoring;
        }
    }

    private static void notify(final WorldServer world, final Chunk chunk, final BlockPos pos,
                               final IBlockState before, final IBlockState after) {
        try {
            world.markAndNotifyBlock(pos, chunk, before, after, NOTIFY);
        } catch (final RuntimeException failure) {
            NovaEngineeringCore.log.error("Construction block update failed at {}", pos, failure);
        }
    }

    boolean place(final EntityPlayerMP player, final WorldServer world, final Chunk chunk, final BlockPos pos,
                  final ItemStack tool, final IBlockState before, final IBlockState desired) {
        if (before.equals(desired)) return false;
        final boolean fast = isStatic(before.getBlock()) && isStatic(desired.getBlock());
        if (!fast) flush(world);
        if (!accepts(world) || !authorized(player, world, pos, tool) || !stillCurrent(world, chunk, pos, before))
            return false;
        if (before.getBlock() != Blocks.AIR && !before.getBlock().isAir(before, world, pos)) {
            final var event = new BlockEvent.BreakEvent(world, pos, before, player);
            event.setExpToDrop(0);
            if (MinecraftForge.EVENT_BUS.post(event) || !accepts(world) || !stillCurrent(world, chunk, pos, before))
                return false;
        }
        final var snapshot = before.getBlock() == Blocks.AIR
            ? new BlockSnapshot(world, pos, before, null) : new BlockSnapshot(world, pos, before);
        final long oldLight = fast && !CHUNK_HOOKS_LIGHT ? lightProperties(world, pos, before) : 0;
        boolean accepted = false;
        try {
            if (!write(world, chunk, pos, desired, fast) || !chunk.getBlockState(pos).equals(desired)) return false;
            if (ForgeEventFactory.onPlayerBlockPlace(player, snapshot, EnumFacing.UP, EnumHand.MAIN_HAND).isCanceled()
                || !authorized(player, world, pos, tool) || !stillCurrent(world, chunk, pos, desired)) return false;
            accepted = true;
        } finally {
            if (!accepted && stillCurrent(world, chunk, pos, desired)) {
                restore(snapshot, world, chunk, pos, before, fast, oldLight);
            }
        }
        if (fast) {
            worlds.add(world);
            positions.add(pos);
            chunks.add(chunk);
            previous.add(before);
            placed.add(desired);
            needsLight.add(!CHUNK_HOOKS_LIGHT && oldLight != lightProperties(world, pos, desired));
            if (positions.size() >= BATCH_SIZE) flush(world);
        } else {
            notify(world, chunk, pos, before, desired);
        }
        return true;
    }

    void flush(final WorldServer world) {
        if (flushing) return;
        flushing = true;
        try {
            int retained = 0;
            for (int i = 0, size = positions.size(); i < size; i++) {
                final var targetWorld = worlds.get(i);
                if (targetWorld != world) {
                    if (retained != i) {
                        worlds.set(retained, targetWorld);
                        positions.set(retained, positions.get(i));
                        chunks.set(retained, chunks.get(i));
                        previous.set(retained, previous.get(i));
                        placed.set(retained, placed.get(i));
                        needsLight.set(retained, needsLight.getBoolean(i));
                    }
                    retained++;
                    continue;
                }
                final var chunk = chunks.get(i);
                final var pos = positions.get(i);
                if (!chunk.isLoaded() || targetWorld.getChunkProvider().getLoadedChunk(chunk.x, chunk.z) != chunk)
                    continue;
                if (!CHUNK_HOOKS_LIGHT && needsLight.getBoolean(i)) {
                    try {
                        targetWorld.checkLight(pos);
                    } catch (final RuntimeException failure) {
                        NovaEngineeringCore.log.error("Construction lighting failed at {}", pos, failure);
                    }
                }
                final var state = placed.get(i);
                if (chunk.getBlockState(pos).equals(state)) notify(targetWorld, chunk, pos, previous.get(i), state);
            }
            worlds.size(retained);
            positions.size(retained);
            chunks.size(retained);
            previous.size(retained);
            placed.size(retained);
            needsLight.size(retained);
        } finally {
            flushing = false;
        }
    }
}
