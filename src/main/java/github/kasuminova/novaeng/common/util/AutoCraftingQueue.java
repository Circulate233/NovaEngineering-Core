package github.kasuminova.novaeng.common.util;

import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.util.AEPartLocation;
import appeng.container.ContainerOpenContext;
import appeng.core.AELog;
import appeng.me.helpers.PlayerSource;
import appeng.util.item.AEItemStack;
import com.circulation.random_complement.common.util.MEHandler;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.CommonProxy;
import github.kasuminova.novaeng.common.container.ContainerNEWCraftConfirm;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Future;

public final class AutoCraftingQueue {

    private static final Map<EntityPlayer, AutoCraftingQueue> QUEUES = new Object2ObjectOpenHashMap<>();
    private Queue<ItemStack> queue = EmptyQueue.empty();

    public static void setQueueAndStrat(final Queue<ItemStack> items, final EntityPlayer player) {
        final AutoCraftingQueue queue = QUEUES.computeIfAbsent(player, ignored -> new AutoCraftingQueue());
        queue.queue = items;
        queue.executionQueue(player);
    }

    @Nullable
    public static AutoCraftingQueue getQueue(final EntityPlayer player) {
        return QUEUES.get(player);
    }

    public static void removeQueue(final EntityPlayer player) {
        final AutoCraftingQueue queue = QUEUES.remove(player);
        if (queue != null) {
            queue.queue.clear();
            queue.queue = EmptyQueue.empty();
        }
    }

    public static void removeWorld(final World world) {
        final var entries = QUEUES.entrySet().iterator();
        while (entries.hasNext()) {
            final var entry = entries.next();
            if (entry.getKey().world == world) {
                entry.getValue().queue.clear();
                entry.getValue().queue = EmptyQueue.empty();
                entries.remove();
            }
        }
    }

    public static void clearAll() {
        for (final AutoCraftingQueue queue : QUEUES.values()) {
            queue.queue.clear();
            queue.queue = EmptyQueue.empty();
        }
        QUEUES.clear();
    }

    public void clearQueue() {
        this.queue.clear();
        this.queue = EmptyQueue.empty();
        QUEUES.values().removeIf(existing -> existing == this);
    }

    public boolean executionQueue(final EntityPlayer player) {
        final ItemStack item = this.queue.poll();
        if (item == null) {
            this.clearQueue();
            return false;
        }
        Future<ICraftingJob> future = null;
        try {
            final var terminal = MEHandler.getTerminalGuiObject(player);
            final var node = terminal == null ? null : terminal.getActionableNode();
            final var grid = node == null ? null : node.getGrid();
            if (grid == null) {
                this.clearQueue();
                return false;
            }
            final ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
            final AEItemStack request = AEItemStack.fromItemStack(item);
            future = crafting.beginCraftingJob(player.world, grid, new PlayerSource(player, terminal),
                request == null ? null : request.setStackSize(item.getCount()), null);
            player.openGui(NovaEngineeringCore.instance, CommonProxy.GuiType.AUTO_CRAFTGUI.ordinal(), player.world,
                terminal.getInventorySlot(), terminal.isBaubleSlot() ? 1 : 0, Integer.MIN_VALUE);
            if (player.openContainer instanceof ContainerNEWCraftConfirm confirm) {
                final ContainerOpenContext context = new ContainerOpenContext(null);
                context.setSide(AEPartLocation.INTERNAL);
                confirm.setOpenContext(context);
                confirm.setAutoStart(false);
                confirm.setJob(future);
                confirm.detectAndSendChanges();
            }
            return true;
        } catch (final Throwable failure) {
            if (future != null) {
                future.cancel(true);
            }
            this.clearQueue();
            AELog.debug(failure);
            return false;
        }
    }
}
