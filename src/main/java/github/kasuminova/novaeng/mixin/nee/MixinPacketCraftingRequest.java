package github.kasuminova.novaeng.mixin.nee;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.security.IActionHost;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.core.AELog;
import com.github.vfyjxf.nee.network.NEEGuiHandler;
import com.github.vfyjxf.nee.network.packet.PacketCraftingRequest;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.concurrent.Future;

@Mixin(value = PacketCraftingRequest.Handler.class,remap = false)
public abstract class MixinPacketCraftingRequest implements IMessageHandler<PacketCraftingRequest, IMessage> {

    /**
     * @author Circulation_
     * @reason 使NEE兼容ae2uel的无线合成终端
     */
    @Overwrite
    private void handlerCraftingTermRequest(AEBaseContainer container, PacketCraftingRequest message, IGrid grid, IActionHost ah, EntityPlayerMP player) {
        if (message.getRequireToCraftStack() != null) {
            Future<ICraftingJob> futureJob = null;
            try {
                final ICraftingGrid cg = grid.getCache(ICraftingGrid.class);
                futureJob = cg.beginCraftingJob(player.world, grid, container.getActionSource(), message.getRequireToCraftStack(), null);

                final ContainerOpenContext context = container.getOpenContext();
                if (context != null) {
                    final TileEntity te = context.getTile();
                    if (te == null) {
                        if (container instanceof IInventorySlotAware slotAware) {
                            NEEGuiHandler.openWirelessGui(player, 201, slotAware.getInventorySlot(), slotAware.isBaubleSlot());
                        }
                    } else {
                        NEEGuiHandler.openGui(player, NEEGuiHandler.CONFIRM_WRAPPER_ID, te, context.getSide());
                    }
                    if (player.openContainer instanceof ContainerCraftConfirm ccc) {
                        ccc.setAutoStart(message.isAutoStart());
                        ccc.setJob(futureJob);
                        ccc.detectAndSendChanges();
                    }
                }
            } catch (final Throwable e) {
                if (futureJob != null) {
                    futureJob.cancel(true);
                }
                AELog.debug(e);
            }
        }
    }
}
