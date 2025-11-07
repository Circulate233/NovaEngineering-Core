package github.kasuminova.novaeng.common.util

import appeng.api.networking.crafting.ICraftingGrid
import appeng.api.networking.crafting.ICraftingJob
import appeng.api.util.AEPartLocation
import appeng.container.ContainerOpenContext
import appeng.core.AELog
import appeng.me.helpers.PlayerSource
import appeng.util.item.AEItemStack
import com.circulation.random_complement.common.util.MEHandler
import github.kasuminova.novaeng.NovaEngineeringCore
import github.kasuminova.novaeng.common.CommonProxy
import github.kasuminova.novaeng.common.container.ContainerNEWCraftConfirm
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import java.util.Queue
import java.util.concurrent.Future

class AutoCraftingQueue {

    companion object {
        private val allQueue = Object2ObjectOpenHashMap<EntityPlayer, AutoCraftingQueue>()

        fun setQueueAndStrat(itemQueue: Queue<ItemStack>, player: EntityPlayer) {
            val q: AutoCraftingQueue = allQueue.computeIfAbsent(player) { p -> AutoCraftingQueue() }
            q.queue = itemQueue
            q.executionQueue(player)
        }

        fun getQueue(player: EntityPlayer): AutoCraftingQueue? {
            return allQueue[player]
        }
    }

    private var queue: Queue<ItemStack> = EmptyQueue.empty()

    fun clearQueue() {
        queue.clear()
    }

    fun executionQueue(player: EntityPlayer): Boolean {
        queue.poll()?.let { item ->
            val obj = MEHandler.getTerminalGuiObject(player)
            obj?.actionableNode?.let { node ->
                val g = node.grid
                var futureJob: Future<ICraftingJob?>? = null

                try {
                    val cg: ICraftingGrid = g.getCache(ICraftingGrid::class.java)
                    futureJob = cg.beginCraftingJob(
                        player.world,
                        g,
                        PlayerSource(player, obj),
                        AEItemStack.fromItemStack(item)?.setStackSize(item.count.toLong()),
                        null
                    )

                    player.openGui(
                        NovaEngineeringCore.instance,
                        CommonProxy.GuiType.AUTO_CRAFTGUI.ordinal,
                        player.world,
                        obj.inventorySlot,
                        if (obj.isBaubleSlot) 1 else 0,
                        Int.MIN_VALUE
                    )

                    val ccc = player.openContainer

                    if (ccc is ContainerNEWCraftConfirm) {
                        val ctx = ContainerOpenContext(null)
                        ctx.side = AEPartLocation.INTERNAL

                        ccc.openContext = ctx
                        ccc.isAutoStart = false
                        ccc.setJob(futureJob)
                        ccc.detectAndSendChanges()
                    }
                    return true
                } catch (e: Throwable) {
                    futureJob?.cancel(true)
                    AELog.debug(e)
                }

            }
        }
        return false
    }
}