package github.kasuminova.novaeng.mixin.fluxnetworks;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sonar.fluxnetworks.api.energy.IItemEnergyHandler;

@Mixin(targets = "sonar.fluxnetworks.common.connection.transfer.FluxControllerHandler$WirelessHandler",remap = false)
public class MixinWirelessHandler {

    @Redirect(method = "chargeItems",at = @At(value = "INVOKE", target = "Lsonar/fluxnetworks/api/energy/IItemEnergyHandler;addEnergy(JLnet/minecraft/item/ItemStack;Z)J"))
    private long chargeItemsRedirect(IItemEnergyHandler instance, long l, ItemStack itemStack, boolean b) {
        return Math.max(instance.addEnergy(l,itemStack,b),0);
    }
}
