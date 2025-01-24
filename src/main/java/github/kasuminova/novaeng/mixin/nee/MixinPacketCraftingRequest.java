package github.kasuminova.novaeng.mixin.nee;

import appeng.api.networking.security.IActionHost;
import appeng.container.interfaces.IInventorySlotAware;
import com.github.vfyjxf.nee.network.NEEGuiHandler;
import com.github.vfyjxf.nee.network.packet.PacketCraftingRequest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PacketCraftingRequest.Handler.class,remap = false)
public abstract class MixinPacketCraftingRequest implements IMessageHandler<PacketCraftingRequest, IMessage> {

    @Inject(method="unofficialHelper", at = @At(value="HEAD"), cancellable=true)
    private static void unofficialHelper(IActionHost host, EntityPlayer player, CallbackInfo ci) {
        if (host instanceof IInventorySlotAware slotAware) {
            NEEGuiHandler.openWirelessGui(player, NEEGuiHandler.WIRELESS_CRAFTING_CONFIRM_UNOFFICIAL_ID, slotAware.getInventorySlot(), slotAware.isBaubleSlot());
             ci.cancel();
        }
    }
}
