package github.kasuminova.novaeng.mixin.ae2;

import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketTerminalUse;
import appeng.items.tools.powered.Terminal;
import baubles.api.BaublesApi;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(value = PacketTerminalUse.class,remap = false)
public class MixinPacketTerminalUse extends AppEngPacket {

    @Shadow
    Terminal terminal;
    @Shadow
    void openGui(ItemStack itemStack, int slotIdx, EntityPlayer player, boolean isBauble){}

    @Inject(method="serverPacketData", at = @At(value= "HEAD"), cancellable = true)
    public void serverPacketDataMixin(INetworkInfo manager, AppEngPacket packet, EntityPlayer player, CallbackInfo ci) {
        NonNullList<ItemStack> mainInventory = player.inventory.mainInventory;
        for(int i = 0; i < mainInventory.size(); ++i) {
            ItemStack is = mainInventory.get(i);
            if (is.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL && is.getTagCompound() != null) {
                int mode = novaEngineering_Core$determineMode(terminal.name());
                List<Integer> list = null;
                if (is.getTagCompound().hasKey("modes")) {
                    list = Arrays.stream(is.getTagCompound().getIntArray("modes")).boxed().collect(Collectors.toList());
                }
                if (list != null && list.contains(mode)) {
                    RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChange(player, mode);
                    openGui(is, i, player, false);
                    ci.cancel();
                    return;
                }
            }
        }
    }

    @Inject(method="tryOpenBauble", at = @At(value= "HEAD"), cancellable = true)
    void tryOpenBaubleMixin(EntityPlayer player, CallbackInfo ci) {
        for(int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); ++i) {
            ItemStack is = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (is.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL && is.getTagCompound() != null) {
                int mode = novaEngineering_Core$determineMode(terminal.name());
                List<Integer> list = null;
                if (is.getTagCompound().hasKey("modes")) {
                    list = Arrays.stream(is.getTagCompound().getIntArray("modes")).boxed().collect(Collectors.toList());
                }
                if (list != null && list.contains(mode)) {
                    RegistryItems.WIRELESS_UNIVERSAL_TERMINAL.nbtChange(player, mode);
                    openGui(is, i, player, true);
                    ci.cancel();
                    return;
                }
            }
        }
    }

    @Unique
    private int novaEngineering_Core$determineMode(String value) {
        switch (value){
            case "WIRELESS_CRAFTING_TERMINAL" :
                return 1;
            case "WIRELESS_PATTERN_TERMINAL":
                return 3;
            case "WIRELESS_FLUID_TERMINAL" :
                return 2;
            default:return 0;
        }
    }
}
