package github.kasuminova.novaeng.mixin.ae2exttable;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiCraftingStatus;
import appeng.helpers.WirelessTerminalGuiObject;
import com._0xc4de.ae2exttable.client.gui.AE2ExtendedGUIs;
import com._0xc4de.ae2exttable.network.ExtendedTerminalNetworkHandler;
import com._0xc4de.ae2exttable.network.packets.PacketSwitchGui;
import github.kasuminova.novaeng.common.item.ItemWirelessUniversalTerminal;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=GuiCraftingStatus.class, remap=false)
public class MixinCraftingCPUStatusTWO {

    @Shadow
    private ItemStack myIcon;

    @Unique
    private AE2ExtendedGUIs novaEngineering_Core$extendedOriginalGui;

    @SuppressWarnings("InjectIntoConstructor")
    @Inject(method="<init>(Lnet/minecraft/entity/player/InventoryPlayer;Lappeng/api/storage/ITerminalHost;)V",
            at= @At(value = "INVOKE", target = "Lappeng/api/definitions/IDefinitions;parts()Lappeng/api/definitions/IParts;", shift = At.Shift.AFTER))
    private void onInit(final InventoryPlayer inventoryPlayer, final ITerminalHost te, CallbackInfo ci) {
        if (te instanceof WirelessTerminalGuiObject wt) {
            ItemStack item = wt.getItemStack();
            if (item.getItem() instanceof ItemWirelessUniversalTerminal bt) {
                if (item.getTagCompound() != null) {
                    switch (item.getTagCompound().getInteger("mode")){
                        case 6,7,8,9:
                            this.novaEngineering_Core$extendedOriginalGui = bt.getGuiType(item);
                            this.myIcon = item;
                    }
                }
            }
        }
    }

    @Inject(method = "actionPerformed", at = @At(value="INVOKE", target="Lappeng/client/gui/implementations/GuiCraftingCPU;actionPerformed(Lnet/minecraft/client/gui/GuiButton;)V", shift=At.Shift.AFTER), cancellable=true, remap=true)
    protected void actionPerformed(GuiButton btn, CallbackInfo ci) {
        if (this.novaEngineering_Core$extendedOriginalGui != null) {
            ExtendedTerminalNetworkHandler.instance().sendToServer(new PacketSwitchGui(this.novaEngineering_Core$extendedOriginalGui));
            ci.cancel();
        }
    }

}
