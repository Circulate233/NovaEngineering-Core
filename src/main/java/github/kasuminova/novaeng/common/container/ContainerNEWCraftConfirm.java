package github.kasuminova.novaeng.common.container;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.security.IActionHost;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.me.helpers.PlayerSource;
import github.kasuminova.novaeng.common.util.AutoCraftingQueue;
import github.kasuminova.novaeng.mixin.ae2.AccessorContainerCraftConfirm;
import github.kasuminova.novaeng.mixin.ae2.AccessorCraftingCPURecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerNEWCraftConfirm extends ContainerCraftConfirm {

    public ContainerNEWCraftConfirm(final InventoryPlayer inventory, final WirelessTerminalGuiObject terminal) {
        super(inventory, terminal);
    }

    @Override
    public boolean canInteractWith(final EntityPlayer player) {
        return true;
    }

    @Override
    public void startJob() {
        if (!((Object) this instanceof AccessorContainerCraftConfirm accessor)) {
            return;
        }
        if (this.getTarget() instanceof IActionHost host && host.getActionableNode() != null) {
            final var grid = host.getActionableNode().getGrid();
            if (grid != null && accessor.getResult() != null && !this.isSimulation()) {
                final ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
                ICraftingCPU cpu = null;
                if (this.getSelectedCpu() != -1) {
                    final Object record = accessor.getCpus().get(this.getSelectedCpu());
                    if (record instanceof AccessorCraftingCPURecord recordAccessor) {
                        cpu = recordAccessor.getCpu();
                    }
                }
                crafting.submitJob(accessor.getResult(), null, cpu, true,
                    new PlayerSource(this.getPlayerInv().player, host));
            }
        }
        final EntityPlayerMP player = (EntityPlayerMP) this.getPlayerInv().player;
        final AutoCraftingQueue queue = AutoCraftingQueue.getQueue(player);
        if (queue != null && !queue.executionQueue(player)) {
            player.closeContainer();
        }
    }
}
