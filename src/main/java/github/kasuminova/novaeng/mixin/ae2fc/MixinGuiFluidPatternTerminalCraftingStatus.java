package github.kasuminova.novaeng.mixin.ae2fc;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiCraftingStatus;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.helpers.WirelessTerminalGuiObject;
import com.glodblock.github.client.GuiFluidPatternTerminalCraftingStatus;
import com.glodblock.github.inventory.GuiType;
import com.glodblock.github.inventory.InventoryHandler;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiFluidPatternTerminalCraftingStatus.class,remap = false)
public class MixinGuiFluidPatternTerminalCraftingStatus extends GuiCraftingStatus {

    @Mutable
    @Final
    @Shadow
    private ITerminalHost part;
    @Shadow
    private GuiTabButton originalGuiBtn;

    public MixinGuiFluidPatternTerminalCraftingStatus(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te);
    }

    @Inject(method="actionPerformed", at = @At(value="INVOKE", target="Lappeng/helpers/WirelessTerminalGuiObject;getItemStack()Lnet/minecraft/item/ItemStack;", shift = At.Shift.AFTER), remap=true)
    protected void onActionPerformed(GuiButton btn, CallbackInfo ci) {
        ItemStack tool = ((WirelessTerminalGuiObject) part).getItemStack();
        if (tool.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
            InventoryHandler.switchGui(GuiType.WIRELESS_FLUID_PATTERN_TERMINAL);
        }
    }
}
