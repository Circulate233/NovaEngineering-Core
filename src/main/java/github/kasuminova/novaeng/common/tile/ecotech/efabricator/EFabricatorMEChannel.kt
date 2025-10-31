package github.kasuminova.novaeng.common.tile.ecotech.efabricator

import appeng.api.networking.GridFlags
import appeng.api.networking.IGridNode
import appeng.api.networking.crafting.ICraftingPatternDetails
import appeng.api.networking.crafting.ICraftingProvider
import appeng.api.networking.crafting.ICraftingProviderHelper
import appeng.api.networking.events.MENetworkChannelsChanged
import appeng.api.networking.events.MENetworkCraftingPatternChange
import appeng.api.networking.events.MENetworkEventSubscribe
import appeng.api.networking.events.MENetworkPowerStatusChange
import appeng.api.networking.security.IActionHost
import appeng.api.networking.security.IActionSource
import appeng.api.util.AECableType
import appeng.api.util.AEPartLocation
import appeng.api.util.DimensionalCoord
import appeng.me.GridAccessException
import appeng.me.helpers.AENetworkProxy
import appeng.me.helpers.IGridProxyable
import appeng.me.helpers.MachineSource
import com.glodblock.github.common.item.ItemFluidPacket
import com.glodblock.github.common.item.fake.FakeItemRegister
import com.glodblock.github.util.FluidCraftingPatternDetails
import github.kasuminova.mmce.common.util.PatternItemFilter
import github.kasuminova.novaeng.common.block.ecotech.efabricator.BlockEFabricatorMEChannel
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorWorker.CraftWork
import hellfirepvp.modularmachinery.ModularMachinery
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.FluidStack
import javax.annotation.Nonnull
import kotlin.math.min

open class EFabricatorMEChannel : EFabricatorPart(), ICraftingProvider, IActionHost, IGridProxyable {

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

                    ci.setCount(stackInSlot.count)
                    return ci
                } else if (!stackInSlot.isEmpty) {
                    stackInSlot.setCount(0)
                    return stackInSlot
                } else return ItemStack.EMPTY
            }
        }
    }

    private val aEProxy: AENetworkProxy = AENetworkProxy(this, "channel", this.visualItemStack, true)
    val source: IActionSource = MachineSource(this)

    private var wasActive = false

    init {
        this.aEProxy.idlePowerUsage = 1.0
        this.aEProxy.setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.DENSE_CAPACITY)
    }

    val visualItemStack: ItemStack
        get() {
            val controller: EFabricatorController? = getController()
            return ItemStack(
                Item.getItemFromBlock(if (controller == null) BlockEFabricatorMEChannel.INSTANCE else controller.parentController!!),
                1,
                0
            )
        }

    @MENetworkEventSubscribe
    fun stateChange(c: MENetworkPowerStatusChange?) {
        postPatternChangeEvent()
    }

    @MENetworkEventSubscribe
    fun stateChange(c: MENetworkChannelsChanged?) {
        postPatternChangeEvent()
    }

    // Crafting Provider
    protected fun postPatternChangeEvent() {
        val currentActive = this.aEProxy.isActive
        if (this.wasActive != currentActive) {
            this.wasActive = currentActive
            try {
                this.aEProxy.grid.postEvent(MENetworkCraftingPatternChange(this, aEProxy.node))
            } catch (ignored: GridAccessException) {
            }
        }
    }

    override fun provideCrafting(craftingTracker: ICraftingProviderHelper) {
        val controller: EFabricatorController = controller ?: return

        val patternBuses: List<EFabricatorPatternBus> = controller.patternBuses
        patternBuses.stream()
            .flatMap { patternBus: EFabricatorPatternBus? ->
                patternBus!!.getDetails().stream()
            }
            .filter { details: ICraftingPatternDetails? -> details!!.isCraftable || details is FluidCraftingPatternDetails }
            .forEach { details: ICraftingPatternDetails? -> craftingTracker.addCraftingOption(this, details) }
    }

    override fun pushPattern(pattern: ICraftingPatternDetails, table: InventoryCrafting): Boolean {
        if (isBusy()) {
            return false
        }

        if (!pattern.isCraftable) {
            if (pattern is FluidCraftingPatternDetails) {
                return pushFluidPattern(pattern, table)
            }
            return false
        }

        val output = pattern.getOutput(table, this.getWorld())
        if (output.isEmpty) {
            return false
        }

        val remaining = Array<ItemStack>(9) { i -> ItemStack.EMPTY }
        var size = 0
        for (i in 0..<min(table.sizeInventory, 9)) {
            val item = table.getStackInSlot(i)
            if (item.isEmpty) {
                remaining[i] = ItemStack.EMPTY
            } else {
                if (size == 0) {
                    size = item.count
                }
                remaining[i] = getContainerItem(item)
            }
        }

        output.setCount(output.count * size)

        return partController.offerWork(CraftWork(remaining, output, size))
    }

    protected fun pushFluidPattern(pattern: FluidCraftingPatternDetails, table: InventoryCrafting): Boolean {
        val outputs = pattern.outputs
        val output =
            if (outputs[0] != null) outputs[0]!!.getCachedItemStack(outputs[0]!!.stackSize) else ItemStack.EMPTY

        if (output.isEmpty) return false

        val remaining = Array<ItemStack>(9) { i -> ItemStack.EMPTY }
        var size = 0
        for (i in 0..<min(table.sizeInventory, 9)) {
            val item = table.getStackInSlot(i)
            if (item.isEmpty) {
                remaining[i] = ItemStack.EMPTY
            } else {
                if (size == 0) {
                    size = item.count
                    if (item.item is ItemFluidPacket) {
                        val amount = (FakeItemRegister.getStack<Any?>(item) as FluidStack).amount
                        val pamount = (FakeItemRegister.getStack<Any?>(pattern.inputs[i]) as FluidStack).amount
                        size = amount / pamount
                    }
                }
                remaining[i] = getContainerItem(item)
            }
        }

        output.setCount(output.count * size)

        return partController.offerWork(CraftWork(remaining, output, size))
    }

    fun insertPattern(patternStack: ItemStack): Boolean {
        if (!PatternItemFilter.INSTANCE.allowInsert(null, -1, patternStack)) {
            return false
        }
        if (partController != null) {
            return partController.insertPattern(patternStack)
        }
        return false
    }

    override fun isBusy(): Boolean {
        if (partController != null) {
            return partController.isQueueFull
        }
        return true
    }

    // Misc
    @Nonnull
    override fun getActionableNode(): IGridNode {
        return aEProxy.node
    }

    @Nonnull
    override fun getProxy(): AENetworkProxy {
        return aEProxy
    }

    @Nonnull
    override fun getLocation(): DimensionalCoord {
        return DimensionalCoord(this)
    }

    override fun gridChanged() {
    }

    override fun getGridNode(@Nonnull dir: AEPartLocation): IGridNode? {
        return aEProxy.node
    }

    @Nonnull
    override fun getCableConnectionType(@Nonnull dir: AEPartLocation): AECableType {
        return AECableType.DENSE_SMART
    }

    override fun securityBreak() {
        getWorld().destroyBlock(getPos(), true)
    }

    override fun readCustomNBT(compound: NBTTagCompound?) {
        super.readCustomNBT(compound)
        aEProxy.readFromNBT(compound)
    }

    override fun writeCustomNBT(compound: NBTTagCompound?) {
        super.writeCustomNBT(compound)
        aEProxy.writeToNBT(compound)
    }

    override fun onChunkUnload() {
        super.onChunkUnload()
        aEProxy.onChunkUnload()
    }

    override fun invalidate() {
        super.invalidate()
        aEProxy.invalidate()
    }

    override fun onAssembled() {
        super.onAssembled()
        aEProxy.setVisualRepresentation(this.visualItemStack)
        ModularMachinery.EXECUTE_MANAGER.addSyncTask {
            aEProxy.onReady()
            partController.recalculateEnergyUsage()
        }
    }

    override fun onDisassembled() {
        super.onDisassembled()
        aEProxy.setVisualRepresentation(this.visualItemStack)
        aEProxy.invalidate()
    }
}