package github.kasuminova.novaeng.common.network

import ae2.api.crafting.IPatternDetails
import ae2.api.crafting.PatternDetailsHelper
import ae2.api.networking.security.IActionHost
import ae2.container.me.patternencode.ContainerPatternEncodingTerm
import ae2.container.SlotSemantics
import ae2.core.definitions.AEItems
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorMEChannel
import hellfirepvp.modularmachinery.ModularMachinery
import io.netty.buffer.ByteBuf
import net.minecraft.item.ItemStack
import net.minecraft.util.text.TextComponentTranslation
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class PktPatternTermUploadPattern : IMessage, IMessageHandler<PktPatternTermUploadPattern, IMessage> {
    override fun fromBytes(buf: ByteBuf) {
    }

    override fun toBytes(buf: ByteBuf) {
    }

    override fun onMessage(message: PktPatternTermUploadPattern, ctx: MessageContext): IMessage? {
        val player = ctx.serverHandler.player
        ModularMachinery.EXECUTE_MANAGER.addSyncTask {
            val container = player.openContainer
            if (container !is ContainerPatternEncodingTerm) {
                return@addSyncTask
            }

            val patternStack = container.getSlots(SlotSemantics.ENCODED_PATTERN).firstOrNull()?.getStack()
                ?: return@addSyncTask
            if (patternStack.isEmpty) {
                return@addSyncTask
            }

            val itemObject = container.getTarget() as? IActionHost ?: return@addSyncTask
            val node = itemObject.getActionableNode() ?: return@addSyncTask
            val channelNodes = node.grid().getMachines(EFabricatorMEChannel::class.java)
            if (channelNodes.isEmpty()) {
                return@addSyncTask
            }

            val pattern: IPatternDetails = PatternDetailsHelper.decodePattern(patternStack, player.world)
                ?: return@addSyncTask
            val out = pattern.primaryOutput.what

            for (channel in channelNodes) {
                channel.controller?.let {
                    for (patternBus in it.getPatternBuses()) {
                        if (patternBus.aePatterns.contains(out)) {
                            player.sendMessage(
                                TextComponentTranslation(
                                    "novaeng.efabricator_parallel_proc.tooltip.0"
                                )
                            )
                            player.inventory.placeItemBackInInventory(player.world, ItemStack(AEItems.BLANK_PATTERN.item(), patternStack.count))
                            container.clear()
                            return@addSyncTask
                        }
                    }
                }
            }
            for (channel in channelNodes) {
                if (channel.insertPattern(patternStack)) {
                    container.clear()
                    break
                }
            }
        }
        return null
    }
}
