package github.kasuminova.novaeng.common.assembly;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.FluidUtil;

final class AssemblyHarvest {
    private AssemblyHarvest() {
    }

    static boolean air(final WorldServer world, final BlockPos pos, final IBlockState state) {
        return state.getBlock().isAir(state, world, pos);
    }

    @SuppressWarnings("deprecation")
    static boolean canHarvest(final EntityPlayerMP player, final WorldServer world, final BlockPos pos,
                              final IBlockState state, final ItemStack profile) {
        return state.getBlockHardness(world, pos) >= 0 && (player.isCreative() || AssemblyPlacement.fluid(state)
            || AssemblyMiningTool.canHarvest(profile, state.getBlock().getActualState(state, world, pos), player));
    }

    enum Result {
        SUCCESS("success"), PLAYER_UNAVAILABLE("player_unavailable"), EDIT_DISABLED("edit_disabled"),
        CANNOT_EDIT("cannot_edit"), WORLD_PROTECTED("world_protected"), OUTSIDE_BORDER("outside_border"),
        WORLD_BUSY("world_busy"), CHUNK_UNLOADED("chunk_unloaded"), CHUNK_CHANGED("chunk_changed"),
        STATE_CHANGED("state_changed"), UNBREAKABLE("unbreakable"), TOOL_REQUIRED("tool_required"),
        BREAK_CANCELLED("break_cancelled"), NO_FLUID_HANDLER("no_fluid_handler"), DRAIN_REFUSED("drain_refused"),
        FLUID_REMAINS("fluid_remains"), CALLBACK_FAILED("exception"), REMOVE_REFUSED("remove_refused"),
        SET_AIR_REFUSED("set_air_refused"), BLOCK_REMAINS("block_remains"), BLOCK_UNCHANGED("block_unchanged");

        final String translationKey;

        Result(final String key) {
            translationKey = "message.assembly.harvest." + key;
        }
    }

    private static Result current(final EntityPlayerMP player, final WorldServer world, final Chunk chunk,
                                  final BlockPos pos, final ItemStack builder, final IBlockState state) {
        if (player.world != world || !player.isEntityAlive()) return Result.PLAYER_UNAVAILABLE;
        if (!player.capabilities.allowEdit) return Result.EDIT_DISABLED;
        if (!player.canPlayerEdit(pos, EnumFacing.UP, builder)) return Result.CANNOT_EDIT;
        if (!world.isBlockModifiable(player, pos)) return Result.WORLD_PROTECTED;
        if (!world.getWorldBorder().contains(pos)) return Result.OUTSIDE_BORDER;
        if (!AssemblyPlacement.accepts(world)) return Result.WORLD_BUSY;
        final var loaded = world.getChunkProvider().getLoadedChunk(chunk.x, chunk.z);
        if (loaded == null) return Result.CHUNK_UNLOADED;
        if (loaded != chunk) return Result.CHUNK_CHANGED;
        return chunk.getBlockState(pos).equals(state) ? Result.SUCCESS : Result.STATE_CHANGED;
    }

    @SuppressWarnings("deprecation")
    static Result remove(final EntityPlayerMP player, final WorldServer world, final Chunk chunk, final BlockPos pos,
                         final ItemStack builder, final ItemStack profile, final IBlockState state,
                         final AssemblyMaterials materials) {
        if (air(world, pos, state)) return Result.SUCCESS;
        var result = current(player, world, chunk, pos, builder, state);
        if (result != Result.SUCCESS) return result;
        if (state.getBlockHardness(world, pos) < 0) return Result.UNBREAKABLE;
        final var block = state.getBlock();
        final boolean creative = player.isCreative();
        final boolean fluid = AssemblyPlacement.fluid(state);
        if (!creative && !fluid && !AssemblyMiningTool.canHarvest(profile, block.getActualState(state, world, pos), player))
            return Result.TOOL_REQUIRED;
        // Give callbacks their own stack: tool wear never mutates the saved profile.
        final var tool = creative || fluid ? ItemStack.EMPTY : profile.copy();
        final var event = new BlockEvent.BreakEvent(world, pos, state, player);
        event.setExpToDrop(creative || fluid || block.canSilkHarvest(world, pos, state, player)
            && EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0 ? 0
            : block.getExpDrop(state, world, pos, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool)));
        if (MinecraftForge.EVENT_BUS.post(event)) return Result.BREAK_CANCELLED;
        result = current(player, world, chunk, pos, builder, state);
        if (result != Result.SUCCESS) return result;
        try (final var ignored = materials.captureDrops()) {
            if (fluid && !creative) {
                final var handler = FluidUtil.getFluidHandler(world, pos, EnumFacing.UP);
                if (handler == null) return Result.NO_FLUID_HANDLER;
                final var available = handler.drain(Integer.MAX_VALUE, false);
                result = current(player, world, chunk, pos, builder, state);
                if (result != Result.SUCCESS) return result;
                if (available == null || available.amount <= 0) {
                    if (!world.setBlockToAir(pos)) return Result.SET_AIR_REFUSED;
                    return air(world, pos, chunk.getBlockState(pos)) ? Result.SUCCESS : Result.BLOCK_REMAINS;
                }
                final var drained = handler.drain(available, true);
                if (drained == null || drained.amount <= 0) return Result.DRAIN_REFUSED;
                materials.recoverFluid(drained);
                return !AssemblyPlacement.fluid(chunk.getBlockState(pos)) ? Result.SUCCESS : Result.FLUID_REMAINS;
            }
            final var tile = chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
            if (!block.removedByPlayer(state, world, pos, player, !creative)) return Result.REMOVE_REFUSED;
            block.onPlayerDestroy(world, pos, state);
            world.playEvent(player, 2001, pos, Block.getStateId(state));
            // willHarvest allows the block to defer removal until harvestBlock has finished.
            if (!creative) block.harvestBlock(world, player, pos, state, tile, tool);
            if (chunk.getBlockState(pos).equals(state)) return Result.BLOCK_UNCHANGED;
            if (!creative && event.getExpToDrop() > 0)
                block.dropXpOnBlockBreak(world, pos, event.getExpToDrop());
            return Result.SUCCESS;
        }
    }
}
