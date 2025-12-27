package github.kasuminova.novaeng.common.network

import appeng.api.AEApi
import appeng.api.networking.crafting.ICraftingPatternDetails
import appeng.api.networking.security.IActionHost
import appeng.api.storage.data.IAEItemStack
import appeng.container.implementations.ContainerPatternEncoder
import appeng.items.misc.ItemEncodedPattern
import appeng.me.GridAccessException
import com.glodblock.github.util.FluidCraftingPatternDetails
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorMEChannel
import github.kasuminova.novaeng.mixin.ae2.AccessorContainerPatternEncoder
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
            if (container !is ContainerPatternEncoder) {
                return@addSyncTask
            }

            val patternSlotOUT = (container as AccessorContainerPatternEncoder).getPatternSlotOUT()
            val patternStack = patternSlotOUT.stack
            if (patternStack.isEmpty) {
                return@addSyncTask
            }

            val part = container.part
            val itemObject = (container as AccessorContainerPatternEncoder).getIGuiItemObject()
            val channelNodes = if (part != null) {
                try {
                    part.proxy.grid.getMachines(EFabricatorMEChannel::class.java)
                } catch (_: GridAccessException) {
                    return@addSyncTask
                }
            } else if (itemObject is IActionHost) {
                itemObject.actionableNode.grid.getMachines(EFabricatorMEChannel::class.java)
            } else {
                return@addSyncTask
            }

            if (channelNodes.isEmpty) return@addSyncTask

            val item = patternStack.item
            val out: IAEItemStack? = if (item is ItemEncodedPattern) {
                val pattern: ICraftingPatternDetails = item.getPatternForItem(patternStack, player.world)
                if (pattern.isCraftable || pattern is FluidCraftingPatternDetails) {
                    pattern.condensedOutputs[0]
                } else return@addSyncTask
            } else return@addSyncTask

            for (channelNode in channelNodes) {
                val channel = channelNode.machine as EFabricatorMEChannel
                channel.controller?.let {
                    for (patternBus in it.getPatternBuses()) {
                        if (patternBus.aePatterns.contains(out)) {
                            player.sendMessage(
                                TextComponentTranslation(
                                    "novaeng.efabricator_parallel_proc.tooltip.0"
                                )
                            )
                            player.inventory.placeItemBackInInventory(
                                player.world,
                                AEApi.instance().definitions().materials().blankPattern()
                                    .maybeStack(patternSlotOUT.stack.count).orElse(
                                        ItemStack.EMPTY
                                    )
                            )
                            patternSlotOUT.putStack(ItemStack.EMPTY)
                            return@addSyncTask
                        }
                    }
                }
            }
            for (channelNode in channelNodes) {
                val channel = channelNode.machine as EFabricatorMEChannel
                if (channel.insertPattern(patternStack)) {
                    patternSlotOUT.putStack(ItemStack.EMPTY)
                    break
                }
            }
        }
        return null
    }
}