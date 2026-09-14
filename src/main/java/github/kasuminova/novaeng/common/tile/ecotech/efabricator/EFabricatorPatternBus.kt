package github.kasuminova.novaeng.common.tile.ecotech.efabricator

import ae2.api.crafting.IPatternDetails
import ae2.api.crafting.PatternDetailsHelper
import ae2.api.networking.crafting.ICraftingProvider
import ae2.api.stacks.AEKey
import ae2.util.inv.AppEngInternalInventory
import ae2.util.inv.InternalInventoryHost
import github.kasuminova.novaeng.NovaEngineeringCore
import github.kasuminova.novaeng.common.container.ContainerEFabricatorPatternSearch
import github.kasuminova.novaeng.common.container.data.EFabricatorPatternData
import github.kasuminova.novaeng.common.network.PktEFabricatorPatternSearchGUIUpdate
import hellfirepvp.modularmachinery.ModularMachinery
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.items.CapabilityItemHandler
import java.util.function.Consumer
import javax.annotation.Nonnull

class EFabricatorPatternBus : EFabricatorPart(), InternalInventoryHost {

    companion object {
        const val PATTERN_SLOTS = 12 * 6
    }

    val aePatterns = ObjectOpenHashSet<AEKey>()
    val patterns = AppEngInternalInventory(this, PATTERN_SLOTS, 1)
    private val details = ObjectArrayList<IPatternDetails?>(PATTERN_SLOTS)

    init {
        // Initialize details...
        repeat(PATTERN_SLOTS) { details.add(null) }
    }

    private fun refreshPatterns() {
        for (i in 0..<PATTERN_SLOTS) {
            refreshPattern(i)
        }
        refreshPatternOutputs()
        notifyPatternChanged()
    }

    private fun refreshPattern(slot: Int) {
        details[slot] = null

        val pattern = patterns.getStackInSlot(slot)
        val item = pattern.item
        if (pattern.isEmpty) {
            return
        }

        details[slot] = PatternDetailsHelper.decodePattern(pattern, getWorld())
    }

    fun getDetails(): List<IPatternDetails> {
        return details.filterNotNull()
    }

    val validPatterns: Int
        get() = details.count { it != null }

    override fun saveChangedInventory(inv: AppEngInternalInventory) {
        markNoUpdateSync()
    }

    override fun isClientSide(): Boolean {
        return world != null && world.isRemote
    }

    override fun onChangeInventory(inv: AppEngInternalInventory, slot: Int) {
        refreshPattern(slot)
        sendPatternSearchGUIUpdateToClient(slot)
        refreshPatternOutputs()
        notifyPatternChanged()
    }

    private fun refreshPatternOutputs() {
        aePatterns.clear()
        for (detail in details) {
            detail?.primaryOutput?.what?.let(aePatterns::add)
        }
    }

    private fun notifyPatternChanged() {
        if (this.partController == null) {
            return
        }
        val channel: EFabricatorMEChannel? = this.partController.channel
        if (channel != null && channel.mainNode.isActive) {
            ICraftingProvider.requestUpdate(channel.mainNode)
        }
        this.partController.recalculateEnergyUsage()
    }

    private fun sendPatternSearchGUIUpdateToClient(slot: Int) {
        if (this.partController == null) {
            return
        }

        val players = ObjectArrayList<EntityPlayerMP>()
        world.playerEntities.stream()
            .filter { obj: EntityPlayer -> EntityPlayerMP::class.java.isInstance(obj) }
            .map { obj: EntityPlayer -> EntityPlayerMP::class.java.cast(obj) }
            .forEach { playerMP: EntityPlayerMP ->
                val openContainer = playerMP.openContainer
                if (openContainer is ContainerEFabricatorPatternSearch) {
                    if (openContainer.getOwner() === this.partController) {
                        players.add(playerMP)
                    }
                }
            }

        if (!players.isEmpty) {
            val pktUpdate = PktEFabricatorPatternSearchGUIUpdate(
                PktEFabricatorPatternSearchGUIUpdate.UpdateType.SINGLE,
                EFabricatorPatternData.of(
                    EFabricatorPatternData.PatternData(getPos(), slot, patterns.getStackInSlot(slot))
                )
            )
            players.forEach(Consumer { player: EntityPlayerMP? ->
                NovaEngineeringCore.NET_CHANNEL.sendTo(
                    pktUpdate,
                    player
                )
            })
        }
    }

    override fun validate() {
        super.validate()
        if (FMLCommonHandler.instance().effectiveSide.isServer) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask { this.refreshPatterns() }
        }
    }

    override fun hasCapability(@Nonnull capability: Capability<*>, facing: EnumFacing?): Boolean {
        return capability === CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing)
    }

    override fun <T> getCapability(@Nonnull capability: Capability<T?>, facing: EnumFacing?): T? {
        val cap = CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
        if (capability === cap) {
            return cap.cast<T?>(patterns.toItemHandler())
        }
        return super.getCapability<T?>(capability, facing)
    }

    override fun readCustomNBT(compound: NBTTagCompound) {
        super.readCustomNBT(compound)
        patterns.readFromNBT(compound, "patterns")
        refreshPatterns()
    }

    override fun writeCustomNBT(compound: NBTTagCompound?) {
        super.writeCustomNBT(compound)
        patterns.writeToNBT(compound, "patterns")
    }
}
