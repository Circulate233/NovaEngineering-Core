package github.kasuminova.novaeng.common.tile.ecotech.efabricator

import ae2.api.AECapabilities
import ae2.api.crafting.IPatternDetails
import ae2.api.crafting.PatternDetailsHelper
import ae2.api.networking.GridFlags
import ae2.api.networking.IGridNode
import ae2.api.networking.IGridNodeListener
import ae2.api.networking.IManagedGridNode
import ae2.api.networking.crafting.ICraftingProvider
import ae2.api.stacks.AEItemKey
import ae2.api.stacks.KeyCounter
import ae2.api.networking.security.IActionSource
import ae2.api.util.AECableType
import ae2.me.ManagedGridNode
import ae2.me.helpers.IGridConnectedTile
import ae2.me.helpers.MachineSource
import github.kasuminova.novaeng.common.block.ecotech.efabricator.BlockEFabricatorMEChannel
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorWorker.CraftWork
import hellfirepvp.modularmachinery.ModularMachinery
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

class EFabricatorMEChannel : EFabricatorPart(), ICraftingProvider, IGridConnectedTile {

    companion object {
        private fun getContainerItem(stackInSlot: ItemStack?): ItemStack {
            if (stackInSlot == null) {
                return ItemStack.EMPTY
            } else {
                val i = stackInSlot.item
                if (i != null && i.hasContainerItem(stackInSlot)) {
                    var ci = i.getContainerItem(stackInSlot)
                    if (!ci.isEmpty && ci.isItemStackDamageable && ci.getItemDamage() == ci.maxDamage) {
                        ci = ItemStack.EMPTY
                    }

                    ci.count = stackInSlot.count
                    return ci
                } else if (!stackInSlot.isEmpty) {
                    stackInSlot.count = 0
                    return stackInSlot
                } else return ItemStack.EMPTY
            }
        }
    }

    private val nodeListener = object : IGridNodeListener<EFabricatorMEChannel> {
        override fun onSaveChanges(nodeOwner: EFabricatorMEChannel, node: IGridNode) {
            nodeOwner.saveChanges()
        }

        override fun onStateChanged(nodeOwner: EFabricatorMEChannel, node: IGridNode, state: IGridNodeListener.State) {
            nodeOwner.postPatternChange()
        }
    }

    @JvmField
    val mainNode: IManagedGridNode = ManagedGridNode(this, nodeListener)
        .setIdlePowerUsage(1.0)
        .setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.DENSE_CAPACITY)
        .setVisualRepresentation(this.visualItemStack)
        .setInWorldNode(true)
        .setTagName("channel")
    val source: IActionSource = MachineSource(this)

    private var wasActive = false

    val visualItemStack: ItemStack
        get() {
            val controller: EFabricatorController? = getController()
            return if (controller == null) ItemStack(BlockEFabricatorMEChannel.INSTANCE)
            else ItemStack(controller.parentController)
        }

    private fun postPatternChange() {
        val currentActive = this.mainNode.isActive
        if (this.wasActive != currentActive) {
            this.wasActive = currentActive
            ICraftingProvider.requestUpdate(mainNode)
        }
    }

    override fun getAvailablePatterns(): List<IPatternDetails> {
        return controller?.getPatternBuses()?.flatMap { it.getDetails() } ?: emptyList()
    }

    override fun pushPattern(pattern: IPatternDetails, inputHolder: Array<KeyCounter>, multiplier: Int): Boolean {
        if (isBusy()) {
            return false
        }

        val outputKey = pattern.primaryOutput.what as? AEItemKey ?: return false
        if (inputHolder.size > 9 || multiplier <= 0) {
            return false
        }

        val remaining = Array<ItemStack>(9) { i -> ItemStack.EMPTY }
        for (i in inputHolder.indices) {
            val entries = inputHolder[i].toList()
            if (entries.size != 1 || entries[0].longValue > Int.MAX_VALUE) {
                return false
            }
            val key = entries[0].key as? AEItemKey ?: return false
            remaining[i] = getContainerItem(key.toStack(entries[0].longValue.toInt()))
        }

        val outputAmount = Math.multiplyExact(pattern.primaryOutput.amount, multiplier.toLong())
        if (outputAmount > Int.MAX_VALUE) {
            return false
        }
        return partController?.offerWork(CraftWork(remaining, outputKey.toStack(outputAmount.toInt()), multiplier)) ?: false
    }

    fun insertPattern(patternStack: ItemStack): Boolean {
        if (!PatternDetailsHelper.isEncodedPattern(patternStack)) {
            return false
        }
        if (partController != null) {
            return partController.insertPattern(patternStack)
        }
        return false
    }

    override fun isBusy(): Boolean {
        if (partController != null) {
            return partController.isQueueFull()
        }
        return true
    }

    override fun readCustomNBT(compound: NBTTagCompound?) {
        super.readCustomNBT(compound)
        mainNode.loadFromNBT(compound)
    }

    override fun writeCustomNBT(compound: NBTTagCompound?) {
        super.writeCustomNBT(compound)
        mainNode.saveToNBT(compound)
    }

    override fun onChunkUnload() {
        super.onChunkUnload()
        mainNode.destroy()
    }

    override fun invalidate() {
        super.invalidate()
        mainNode.destroy()
    }

    override fun onAssembled() {
        super.onAssembled()
        mainNode.setVisualRepresentation(this.visualItemStack)
        ModularMachinery.EXECUTE_MANAGER.addSyncTask {
            if (!mainNode.isReady() && !getWorld().isRemote) {
                mainNode.create(getWorld(), getPos())
            }
            partController?.recalculateEnergyUsage()
        }
    }

    override fun onDisassembled() {
        super.onDisassembled()
        mainNode.setVisualRepresentation(this.visualItemStack)
        mainNode.destroy()
    }

    override fun getMainNode(): IManagedGridNode = mainNode

    override fun saveChanges() {
        markDirty()
    }

    override fun getCableConnectionType(dir: EnumFacing): AECableType = AECableType.DENSE_SMART

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean {
        return capability === AECapabilities.IN_WORLD_GRID_NODE_HOST || super.hasCapability(capability, facing)
    }

    override fun <T> getCapability(capability: Capability<T?>, facing: EnumFacing?): T? {
        if (capability === AECapabilities.IN_WORLD_GRID_NODE_HOST) {
            return AECapabilities.IN_WORLD_GRID_NODE_HOST.cast(this)
        }
        return super.getCapability(capability, facing)
    }
}