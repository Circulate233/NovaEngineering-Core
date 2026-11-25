package github.kasuminova.novaeng.common.handler;

import github.kasuminova.novaeng.client.util.NEWBlockArrayPreviewRenderHelper;
import github.kasuminova.novaeng.common.assembly.AssemblyConstructionTasks;
import github.kasuminova.novaeng.common.item.ItemMachineAssemblyTool;
import github.kasuminova.novaeng.common.util.AutoCraftingQueue;
import github.kasuminova.novaeng.common.util.NEWMachineAssemblyManager;
import hellfirepvp.modularmachinery.client.ClientScheduler;
import hellfirepvp.modularmachinery.client.util.DynamicMachineRenderContext;
import hellfirepvp.modularmachinery.common.block.BlockController;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class MachineAssemblyHandler {
    public static final MachineAssemblyHandler INSTANCE = new MachineAssemblyHandler();

    private MachineAssemblyHandler() {
    }

    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public void onPlayerRightBlock(final PlayerInteractEvent.RightClickBlock event) {
        final var hand = event.getHand();
        if (hand != EnumHand.MAIN_HAND) {
            return;
        }
        final var world = event.getWorld();
        final var player = event.getEntityPlayer();
        final var stack = player.getHeldItem(hand);
        if (player.isSneaking() || !(stack.getItem() instanceof ItemMachineAssemblyTool)) {
            return;
        }
        final var pos = event.getPos();
        final var tile = world.getTileEntity(pos);
        var state = world.getBlockState(pos);
        final var block = state.getBlock();
        state = block.getActualState(state, world, pos);
        EnumFacing facing = null;
        DynamicMachine machine = null;
        if (tile instanceof TileMultiblockMachineController controller) {
            machine = controller.getBlueprintMachine();
            if (machine == null) {
                if (block instanceof BlockController controllerBlock) {
                    machine = controllerBlock.getParentMachine();
                } else {
                    return;
                }
            }
        } else {
            final int meta = block.getMetaFromState(state);
            final var iterator = NEWMachineAssemblyManager.getConstructorsIterator();
            while (iterator.hasNext()) {
                final var entry = iterator.next();
                if (entry.getKey() == block && entry.getValue().containsKey(meta)) {
                    machine = entry.getValue().get(meta);
                    final var property = block.getBlockState().getProperty("facing");
                    final Object value = property == null ? null : state.getValue(property);
                    facing = value instanceof EnumFacing direction ? direction : event.getFace();
                    break;
                }
            }
            if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) {
                facing = EnumFacing.NORTH;
            }
            if (machine == null) {
                return;
            }
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (world.isRemote) {
            final var preview = NEWBlockArrayPreviewRenderHelper.INSTANCE;
            if (preview.getWork()) {
                final long old = preview.getKey();
                preview.unloadWorld();
                if (old == pos.toLong()) {
                    return;
                }
            }
            final var render = DynamicMachineRenderContext.createContext(machine, ItemMachineAssemblyTool.getDynamicPatternSize(stack));
            render.setShiftSnap(ClientScheduler.getClientTick());
            preview.startPreview(render, pos, facing);
        }
    }

    @SubscribeEvent
    public void onPlayerLeftBlock(final PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntityPlayer().isSneaking()
            && event.getEntityPlayer().getHeldItemMainhand().getItem() instanceof ItemMachineAssemblyTool
            && event.getEntityPlayer() instanceof EntityPlayerMP player
            && AssemblyConstructionTasks.cancelAt(player, event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        final var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) AssemblyConstructionTasks.tick(server);
    }

    @SubscribeEvent
    public void onPlayerLogOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP player) {
            AssemblyConstructionTasks.cancel(player);
            AutoCraftingQueue.removeQueue(player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangeDim(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP player) {
            AssemblyConstructionTasks.cancel(player);
            AutoCraftingQueue.removeQueue(player);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(final WorldEvent.Unload event) {
        if (event.getWorld() instanceof WorldServer world) AssemblyConstructionTasks.unload(world);
    }
}
