package github.kasuminova.novaeng.client.gui

import appeng.client.gui.implementations.GuiCraftConfirm
import appeng.helpers.WirelessTerminalGuiObject
import com.circulation.random_complement.client.RCGuiCraftConfirm
import github.kasuminova.novaeng.NovaEngineeringCore
import github.kasuminova.novaeng.common.network.PktAutoCraftConfirm
import github.kasuminova.novaeng.mixin.ae2.AccessorGuiCraftConfirm
import java.io.IOException
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.InventoryPlayer

open class GuiNEWCraftConfirm(ip: InventoryPlayer, te: WirelessTerminalGuiObject) : GuiCraftConfirm(ip, te) {

    var cancel: GuiButton? = null
    var start: GuiButton? = null
    val utils = this as AccessorGuiCraftConfirm

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun initGui() {
        super.initGui()
        cancel = utils.`n$getCancel`()
        start = utils.`n$getStart`()
    }

    @Throws(IOException::class)
    override fun actionPerformed(btn: GuiButton) {
        if (btn == cancel) {
            if (this is RCGuiCraftConfirm && isShiftKeyDown()) {
                `rc$addMissBookmark`()
            }
            NovaEngineeringCore.NET_CHANNEL.sendToServer(PktAutoCraftConfirm())
            this.mc.player.closeScreenAndDropStack()
            return
        }
        super.actionPerformed(btn)
        if (btn == start) {
            this.mc.player.closeScreenAndDropStack()
        }
    }

}