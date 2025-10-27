package github.kasuminova.novaeng.common.container

import appeng.api.networking.crafting.ICraftingGrid
import appeng.api.networking.security.IActionHost
import appeng.container.implementations.ContainerCraftConfirm
import appeng.helpers.WirelessTerminalGuiObject
import appeng.me.helpers.PlayerSource
import github.kasuminova.novaeng.common.util.AutoCraftingQueue
import github.kasuminova.novaeng.mixin.ae2.AccessorContainerCraftConfirm
import github.kasuminova.novaeng.mixin.ae2.AccessorCraftingCPURecord
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.entity.player.InventoryPlayer

class ContainerNEWCraftConfirm(ip: InventoryPlayer, te: WirelessTerminalGuiObject) : ContainerCraftConfirm(ip, te) {

    override fun startJob() {
        @Suppress("USELESS_IS_CHECK")
        if (this is AccessorContainerCraftConfirm) {
            val h = this.target as? IActionHost
            h?.actionableNode?.grid?.let { grid ->
                if (this.`n$getResult`() != null && !this.isSimulation) {
                    val cc = grid.getCache<ICraftingGrid>(ICraftingGrid::class.java)
                    cc.submitJob(
                        this.`n$getResult`(),
                        null,
                        if (this.getSelectedCpu() == -1) null
                        else {
                            val c = this.`n$getCpus`()[this.getSelectedCpu()]
                            if (c is AccessorCraftingCPURecord) {
                                c.`n$getCpu`()
                            } else null
                        },
                        true,
                        PlayerSource(this.playerInv.player, h)
                    )
                }
            }
            val player = playerInv.player as EntityPlayerMP
            AutoCraftingQueue.getQueue(player)?.let {
                if (!it.executionQueue(player)) {
                    player.closeContainer()
                }
            }
        }
    }

}