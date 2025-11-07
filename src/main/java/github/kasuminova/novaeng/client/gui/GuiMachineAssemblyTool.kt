package github.kasuminova.novaeng.client.gui

import com.brandon3055.brandonscore.inventory.PlayerSlot
import com.brandon3055.draconicevolution.client.gui.toolconfig.GuiConfigureTool
import com.brandon3055.draconicevolution.client.gui.toolconfig.GuiToolConfig
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumHand

class GuiMachineAssemblyTool(player: EntityPlayer) : GuiConfigureTool(
    GuiToolConfig(player), player, player.getHeldItem(EnumHand.MAIN_HAND),
    PlayerSlot(player.inventory.currentItem, PlayerSlot.EnumInvCategory.MAIN)
) {

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == 1 || this.mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
            this.mc.player.closeScreen()
        }
    }
}