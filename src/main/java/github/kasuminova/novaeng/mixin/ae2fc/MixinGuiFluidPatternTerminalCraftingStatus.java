package github.kasuminova.novaeng.mixin.ae2fc;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiCraftingStatus;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.helpers.WirelessTerminalGuiObject;
import com.glodblock.github.client.GuiFluidPatternTerminalCraftingStatus;
import com.glodblock.github.common.part.PartExtendedFluidPatternTerminal;
import com.glodblock.github.common.part.PartFluidPatternTerminal;
import com.glodblock.github.inventory.GuiType;
import com.glodblock.github.inventory.InventoryHandler;
import com.glodblock.github.loader.FCItems;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;

import java.io.IOException;

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

    /**
     * @author Circulation_
     * @reason 使得无线通用终端可以被返回
     */
    @Overwrite
    protected void actionPerformed(final GuiButton btn) throws IOException {
        if (btn == originalGuiBtn) {
            if (part instanceof WirelessTerminalGuiObject) {
                ItemStack tool = ((WirelessTerminalGuiObject) part).getItemStack();
                if (tool.getItem() == FCItems.WIRELESS_FLUID_PATTERN_TERMINAL || tool.getItem() == RegistryItems.WIRELESS_UNIVERSAL_TERMINAL) {
                    InventoryHandler.switchGui(GuiType.WIRELESS_FLUID_PATTERN_TERMINAL);
                }
            } else if (part instanceof PartFluidPatternTerminal)
                InventoryHandler.switchGui(GuiType.FLUID_PATTERN_TERMINAL);
            else if (part instanceof PartExtendedFluidPatternTerminal)
                InventoryHandler.switchGui(GuiType.FLUID_EXTENDED_PATTERN_TERMINAL);
        } else {
            super.actionPerformed(btn);
        }
    }
}
