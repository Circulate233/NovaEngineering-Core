package github.kasuminova.novaeng.mixin.ae2fc;

import baubles.api.BaublesApi;
import com.glodblock.github.inventory.GuiType;
import com.glodblock.github.loader.FCItems;
import com.glodblock.github.network.CPacketUseKeybind;
import com.glodblock.github.util.Util;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = CPacketUseKeybind.Handler.class, remap = false)
public class MixinCPacketUseKeybindHandler {
    /**
     * @author Circulation_
     * @reason 修改方法使得允许无线通用终端打开
     */
    @Overwrite
    public IMessage onMessage(CPacketUseKeybind message, MessageContext ctx) {
        final EntityPlayerMP player = ctx.getServerHandler().player;

        player.getServerWorld().addScheduledTask(() -> {
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stackInSlot = player.inventory.getStackInSlot(i);
                if (stackInSlot.getItem() == FCItems.WIRELESS_FLUID_PATTERN_TERMINAL) {
                    Util.openWirelessTerminal(stackInSlot, i, false, player.world, player, GuiType.WIRELESS_FLUID_PATTERN_TERMINAL);
                    return;
                } else if (stackInSlot.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
                    RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChange(player,4);
                    Util.openWirelessTerminal(stackInSlot, i, false, player.world, player, GuiType.WIRELESS_FLUID_PATTERN_TERMINAL);
                    return;
                }
            }
            if (Loader.isModLoaded("baubles")) {
                tryOpenBauble(player);
            }
        });
        return null;
    }

    /**
     * @author Circulation_
     * @reason 同上
     */
    @Overwrite
    @Optional.Method(modid = "baubles")
    private static void tryOpenBauble(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stackInSlot = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (stackInSlot.getItem() == FCItems.WIRELESS_FLUID_PATTERN_TERMINAL) {
                Util.openWirelessTerminal(stackInSlot, i, true, player.world, player, GuiType.WIRELESS_FLUID_PATTERN_TERMINAL);
                return;
            } else if (stackInSlot.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
                RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChange(player,4);
                Util.openWirelessTerminal(stackInSlot, i, true, player.world, player, GuiType.WIRELESS_FLUID_PATTERN_TERMINAL);
                return;
            }
        }
    }
}
