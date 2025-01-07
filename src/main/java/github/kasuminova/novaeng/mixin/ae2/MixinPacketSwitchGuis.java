package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.networking.security.IActionHost;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.util.Platform;
import baubles.api.BaublesApi;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PacketSwitchGuis.class,remap = false)
public class MixinPacketSwitchGuis extends AppEngPacket {

    @Final
    @Shadow
    private GuiBridge newGui;

    /**
     * @author Circulation_
     * @reason 使通用终端在合成计划界面可以正确返回原本的终端模式
     */
    @Overwrite
    public void serverPacketData(final INetworkInfo manager, final AppEngPacket packet, final EntityPlayer player) {
        final Container c = player.openContainer;
        if (c instanceof AEBaseContainer bc) {
            final ContainerOpenContext context = bc.getOpenContext();
            if (context != null) {
                final Object target = bc.getTarget();
                if (target instanceof IActionHost ah) {

                    final TileEntity te = context.getTile();

                    if (te != null) {
                        Platform.openGUI(player, te, bc.getOpenContext().getSide(), this.newGui);
                    } else {
                        if (ah instanceof IInventorySlotAware i) {
                            if (!i.isBaubleSlot()) {
                                ItemStack item = player.inventory.getStackInSlot(i.getInventorySlot());
                                if (item.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
                                    if (item.getTagCompound() != null && item.getTagCompound().getInteger("craft") == 2) {
                                        item.getTagCompound().setInteger("craft",1);
                                        switch (item.getTagCompound().getInteger("mode")) {
                                            case 0:
                                                Platform.openGUI(player, i.getInventorySlot(), this.newGui, false);
                                                break;
                                            case 1:
                                                Platform.openGUI(player, i.getInventorySlot(), GuiBridge.GUI_WIRELESS_CRAFTING_TERMINAL, false);
                                                break;
                                            case 3:
                                                Platform.openGUI(player, i.getInventorySlot(), GuiBridge.GUI_WIRELESS_PATTERN_TERMINAL, false);
                                                break;
                                        }
                                    } else {
                                        if (item.getTagCompound() != null && item.getTagCompound().getInteger("craft") == 1) {
                                            item.getTagCompound().setInteger("craft",2);
                                        }
                                        Platform.openGUI(player, i.getInventorySlot(), this.newGui, false);
                                    }
                                } else {
                                    Platform.openGUI(player, i.getInventorySlot(), this.newGui, false);
                                }
                            } else {
                                ItemStack item = BaublesApi.getBaublesHandler(player).getStackInSlot(i.getInventorySlot());
                                if (item.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
                                    if (item.getTagCompound() != null && item.getTagCompound().getInteger("craft") == 2) {
                                        item.getTagCompound().setInteger("craft",1);
                                        switch (item.getTagCompound().getInteger("mode")) {
                                            case 0:
                                                Platform.openGUI(player, i.getInventorySlot(), this.newGui, true);
                                                break;
                                            case 1:
                                                Platform.openGUI(player, i.getInventorySlot(), GuiBridge.GUI_WIRELESS_CRAFTING_TERMINAL, true);
                                                break;
                                            case 3:
                                                Platform.openGUI(player, i.getInventorySlot(), GuiBridge.GUI_WIRELESS_PATTERN_TERMINAL, true);
                                                break;
                                        }
                                    } else {
                                        if (item.getTagCompound() != null && item.getTagCompound().getInteger("craft") == 1) {
                                            item.getTagCompound().setInteger("craft",2);
                                        }
                                        Platform.openGUI(player, i.getInventorySlot(), this.newGui, true);
                                    }
                                } else {
                                    Platform.openGUI(player, i.getInventorySlot(), this.newGui, true);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
