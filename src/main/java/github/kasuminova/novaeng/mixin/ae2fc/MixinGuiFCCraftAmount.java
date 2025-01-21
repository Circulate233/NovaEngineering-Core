package github.kasuminova.novaeng.mixin.ae2fc;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiCraftAmount;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.AEBaseContainer;
import appeng.helpers.WirelessTerminalGuiObject;
import com.glodblock.github.client.GuiFCCraftAmount;
import com.glodblock.github.inventory.GuiType;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiFCCraftAmount.class,remap = false)
public class MixinGuiFCCraftAmount extends GuiCraftAmount {

    @Shadow
    private GuiType originGui;
    @Shadow
    private GuiTabButton originalGuiBtn;

    public MixinGuiFCCraftAmount(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te);
    }

    @Inject(method="initGui", at = @At(value="INVOKE", target="Lappeng/helpers/WirelessTerminalGuiObject;getItemStack()Lnet/minecraft/item/ItemStack;", shift = At.Shift.AFTER), cancellable=true, remap=true)
    protected void onInitGui(CallbackInfo ci) {
        Object te = ((AEBaseContainer)this.inventorySlots).getTarget();
        ItemStack icon = ItemStack.EMPTY;
        ItemStack tool = ((WirelessTerminalGuiObject) te).getItemStack();
        if (tool.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
            icon = tool;
            this.originGui = GuiType.WIRELESS_FLUID_PATTERN_TERMINAL;
        }
        if (!icon.isEmpty() && this.originGui != null) {
            this.buttonList.remove(this.originalGuiBtn);
            this.buttonList.add(this.originalGuiBtn = new GuiTabButton(this.guiLeft + 154, this.guiTop, icon, icon.getDisplayName(), this.itemRender));
            ci.cancel();
        }
    }

}
