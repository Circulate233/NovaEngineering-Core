package github.kasuminova.novaeng.client.gui;

import com.brandon3055.brandonscore.inventory.PlayerSlot;
import com.brandon3055.draconicevolution.client.gui.toolconfig.GuiConfigureTool;
import com.brandon3055.draconicevolution.client.gui.toolconfig.GuiToolConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;

public final class GuiMachineAssemblyTool extends GuiConfigureTool {

    public GuiMachineAssemblyTool(final EntityPlayer player) {
        super(new GuiToolConfig(player), player, player.getHeldItem(EnumHand.MAIN_HAND),
            new PlayerSlot(player.inventory.currentItem, PlayerSlot.EnumInvCategory.MAIN));
    }

    @Override
    public void setEntryValue(final int id, final boolean value) {
    }

    @Override
    public void setEntryValue(final int id, final float value) {
    }

    @Override
    public void setEntryValue(final int id, final String value) {
    }

    @Override
    protected void keyTyped(final char character, final int keyCode) {
        if (keyCode == 1 || this.mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
            this.mc.player.closeScreen();
        }
    }
}
