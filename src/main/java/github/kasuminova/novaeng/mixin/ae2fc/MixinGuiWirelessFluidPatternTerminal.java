package github.kasuminova.novaeng.mixin.ae2fc;

import com.glodblock.github.inventory.GuiType;
import com.glodblock.github.inventory.InventoryHandler;
import github.kasuminova.novaeng.common.network.CPacketSwitchGuis;
import github.kasuminova.novaeng.NovaEngineeringCore;
import net.minecraftforge.fml.common.network.IGuiHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = InventoryHandler.class,remap = false)
public abstract class MixinGuiWirelessFluidPatternTerminal implements IGuiHandler {

    /**
     * @author Circulation_
     * @reason huhuhuhu
     */
    @Overwrite
    public static void switchGui(GuiType guiType) {
        NovaEngineeringCore.NET_CHANNEL.sendToServer(new CPacketSwitchGuis(guiType));
    }
}
