package github.kasuminova.novaeng.common.tile.ecotech.efabricator

import appeng.api.implementations.ICraftingPatternItem
import appeng.api.networking.crafting.ICraftingPatternDetails
import appeng.api.networking.events.MENetworkCraftingPatternChange
import appeng.api.storage.data.IAEItemStack
import appeng.items.misc.ItemEncodedPattern
import appeng.me.GridAccessException
import appeng.tile.inventory.AppEngInternalInventory
import appeng.util.inv.IAEAppEngInventory
import appeng.util.inv.InvOperation
import com.glodblock.github.util.FluidCraftingPatternDetails
import com.glodblock.github.util.FluidPatternDetails
import github.kasuminova.mmce.common.util.PatternItemFilter
import github.kasuminova.novaeng.NovaEngineeringCore
import github.kasuminova.novaeng.common.container.ContainerEFabricatorPatternSearch
import github.kasuminova.novaeng.common.container.data.EFabricatorPatternData
import github.kasuminova.novaeng.common.network.PktEFabricatorPatternSearchGUIUpdate
import hellfirepvp.modularmachinery.ModularMachinery
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.IItemHandler
import java.util.Objects
import java.util.function.Consumer
import java.util.function.IntFunction
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.annotation.Nonnull

open class EFabricatorPatternBus : EFabricatorPart(), IAEAppEngInventory {

    companion object {
        val PATTERN_SLOTS = 12 * 6
    }

    val aePatterns = ObjectOpenHashSet<IAEItemStack>()
    val patterns = AppEngInternalInventory(this, PATTERN_SLOTS, 1, PatternItemFilter.INSTANCE)
    protected val details = ObjectArrayList<ICraftingPatternDetails?>(PATTERN_SLOTS)

    init {
        // Initialize details...
        IntStream.range(0, PATTERN_SLOTS).mapToObj<ICraftingPatternDetails?>(IntFunction { i: Int -> null })
            .forEach { e: ICraftingPatternDetails? -> details.add(e) }
    }

    protected fun refreshPatterns() {
        for (i in 0..<PATTERN_SLOTS) {
            refreshPattern(i)
        }
        notifyPatternChanged()
    }

    protected fun refreshPattern(slot: Int) {
        details[slot] = null

        val pattern = patterns.getStackInSlot(slot)
        val item = pattern.item
        if (pattern.isEmpty || item !is ICraftingPatternItem) {
            return
        }

        val detail = item.getPatternForItem(pattern, getWorld())
        if (detail != null && (detail.isCraftable || detail is FluidCraftingPatternDetails)) {
            details[slot] = detail
        }
    }

    fun getDetails(): MutableList<ICraftingPatternDetails?> {
        return details.stream()
            .filter { obj: ICraftingPatternDetails? -> Objects.nonNull(obj) }
            .collect(Collectors.toList())
    }

    val validPatterns: Int
        get() = details.stream().filter { obj: ICraftingPatternDetails? -> Objects.nonNull(obj) }.count()
            .toInt()

    override fun saveChanges() {
        markNoUpdateSync()
    }

    override fun onChangeInventory(
        inv: IItemHandler,
        slot: Int,
        mc: InvOperation,
        removedStack: ItemStack,
        newStack: ItemStack
    ) {
        refreshPattern(slot)
        notifyPatternChanged()
        sendPatternSearchGUIUpdateToClient(slot)
        when (mc) {
            InvOperation.EXTRACT -> removePattern(removedStack)
            InvOperation.INSERT -> addPattern(newStack)
            InvOperation.SET -> {
                removePattern(removedStack)
                addPattern(newStack)
            }
        }
    }

    private fun addPattern(stack: ItemStack) {
        val item = stack.item
        if (item is ItemEncodedPattern) {
            val pattern = item.getPatternForItem(stack, this.world) ?: return
            if (pattern.isCraftable || pattern is FluidPatternDetails) {
                aePatterns.add(pattern.condensedOutputs[0])
            }
        }
    }

    private fun removePattern(stack: ItemStack) {
        val item = stack.item
        if (item is ItemEncodedPattern) {
            val pattern = item.getPatternForItem(stack, this.world) ?: return
            if (pattern.isCraftable || pattern is FluidPatternDetails) {
                aePatterns.remove(pattern.condensedOutputs[0])
            }
        }
    }

    private fun notifyPatternChanged() {
        if (this.partController == null) {
            return
        }
        try {
            val channel: EFabricatorMEChannel? = this.partController.channel
            if (channel != null && channel.proxy.isActive) {
                channel.proxy.grid
                    .postEvent(MENetworkCraftingPatternChange(channel, channel.proxy.node))
            }
        } catch (ignored: GridAccessException) {
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
        if (FMLCommonHandler.instance().getEffectiveSide().isServer) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask { this.refreshPatterns() }
        }
    }

    override fun hasCapability(@Nonnull capability: Capability<*>, facing: EnumFacing?): Boolean {
        return capability === CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing)
    }

    override fun <T> getCapability(@Nonnull capability: Capability<T?>, facing: EnumFacing?): T? {
        val cap = CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
        if (capability === cap) {
            return cap.cast<T?>(patterns)
        }
        return super.getCapability<T?>(capability, facing)
    }

    override fun readCustomNBT(compound: NBTTagCompound) {
        super.readCustomNBT(compound)
        patterns.readFromNBT(compound.getCompoundTag("patterns"))
        for (stack in patterns) {
            addPattern(stack)
        }
    }

    override fun writeCustomNBT(compound: NBTTagCompound?) {
        super.writeCustomNBT(compound)
        patterns.writeToNBT(compound, "patterns")
    }
}
