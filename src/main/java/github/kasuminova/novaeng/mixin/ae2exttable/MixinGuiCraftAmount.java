package github.kasuminova.novaeng.mixin.ae2exttable;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftAmount;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.AEBaseContainer;
import appeng.helpers.WirelessTerminalGuiObject;
import com._0xc4de.ae2exttable.client.gui.AE2ExtendedGUIs;
import com._0xc4de.ae2exttable.items.ItemRegistry;
import com._0xc4de.ae2exttable.network.ExtendedTerminalNetworkHandler;
import com._0xc4de.ae2exttable.network.packets.PacketSwitchGui;
import github.kasuminova.novaeng.common.item.ItemWirelessUniversalTerminal;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(value= GuiCraftAmount.class, remap=false)
public abstract class MixinGuiCraftAmount extends AEBaseGui {

    @Shadow
    private GuiTabButton originalGuiBtn;

    @Shadow
    private GuiButton next;

    @Unique
    private AE2ExtendedGUIs novaEngineering_Core$extendedOriginalGui;

    public MixinGuiCraftAmount(Container container) {
        super(container);
    }

    @Inject(method="initGui", at=@At(value="RETURN"), remap=true)
    private void onInitGui(CallbackInfo ci) {
        Object target = ((AEBaseContainer) this.inventorySlots).getTarget();
        if (target instanceof WirelessTerminalGuiObject term)
            if (term.getItemStack().getItem() instanceof ItemWirelessUniversalTerminal item) {
                for (Object btn: new ArrayList<>(this.buttonList)) {
                    if (btn instanceof GuiTabButton b) {
                        this.buttonList.remove(b);
                    }
                }
                this.novaEngineering_Core$extendedOriginalGui = item.getGuiType(term.getItemStack());
                ItemStack myIcon = new ItemStack(ItemRegistry.partByGuiType(this.novaEngineering_Core$extendedOriginalGui));
                this.buttonList.add((this.originalGuiBtn = new GuiTabButton(this.guiLeft + 154, this.guiTop, myIcon, myIcon.getDisplayName(), this.itemRender)));
            }
    }

    @Inject(method="actionPerformed", at = @At(value="INVOKE", target="Lappeng/client/gui/AEBaseGui;actionPerformed(Lnet/minecraft/client/gui/GuiButton;)V", shift = At.Shift.AFTER), cancellable = true, remap=true)
    protected void actionPerformedGuiSwitch(GuiButton btn, CallbackInfo ci) {
        if (btn == this.originalGuiBtn && this.novaEngineering_Core$extendedOriginalGui != null) {
            ExtendedTerminalNetworkHandler.instance().sendToServer(new PacketSwitchGui(this.novaEngineering_Core$extendedOriginalGui));
            ci.cancel();
        }
    }

    @Shadow
    public void drawFG(int i, int i1, int i2, int i3) {

    }
}
