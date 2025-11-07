package github.kasuminova.novaeng.common.util

import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized
import hellfirepvp.modularmachinery.common.util.IItemHandlerImpl
import hellfirepvp.modularmachinery.common.util.ItemUtils
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTPrimitive
import net.minecraft.nbt.NBTTagByte
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraftforge.common.util.Constants
import java.util.Arrays
import java.util.BitSet
import java.util.function.IntConsumer
import java.util.stream.IntStream
import javax.annotation.Nonnull

open class TileItemHandler(
    val owner: TileEntitySynchronized,
    inSlots: IntArray,
    outSlots: IntArray,
    val invName: String
) : IItemHandlerImpl(inSlots, outSlots) {

    companion object {
        @JvmStatic
        fun create(owner: TileEntitySynchronized, slotCount: Int, invName: String): TileItemHandler {
            val slotIDs = IntArray(slotCount)
            for (slotID in slotIDs.indices) {
                slotIDs[slotID] = slotID
            }
            return TileItemHandler(owner, slotIDs, slotIDs, invName).setAllSlotAvailable().updateSlotLimits()
        }
    }

    protected val availableSlots: BitSet = BitSet()

    private var onChangedListener: IntConsumer? = null

    fun updateInOutSlots(): TileItemHandler {
        val slotIDs = IntArray(inventory.size)
        for (slotID in slotIDs.indices) {
            slotIDs[slotID] = slotID
        }
        this.inSlots = slotIDs
        this.outSlots = slotIDs
        return this
    }

    fun updateSlotLimits(): TileItemHandler {
        val slotLimits = IntArray(inventory.size)
        Arrays.fill(slotLimits, 1)
        this.slotLimits = slotLimits
        return this
    }

    fun setOnChangedListener(onChangedListener: IntConsumer?): TileItemHandler {
        this.onChangedListener = onChangedListener
        return this
    }

    fun isSlotAvailable(slotID: Int): Boolean {
        return availableSlots.get(slotID)
    }

    fun setSlotAvailable(slotID: Int): TileItemHandler {
        availableSlots.set(slotID)
        return this
    }

    fun setAllSlotAvailable(): TileItemHandler {
        availableSlots.set(0, inventory.size)
        return this
    }

    fun setUnavailableSlots(slotIDs: IntArray): TileItemHandler {
        for (slotID in slotIDs) {
            availableSlots.set(slotID, false)
        }
        return this
    }

    fun setUnavailableSlot(slotID: Int): TileItemHandler {
        availableSlots.set(slotID, false)
        return this
    }

    val availableSlotsStream: IntStream
        get() = availableSlots.stream()

    override fun setStackInSlot(slot: Int, @Nonnull stack: ItemStack) {
        super.setStackInSlot(slot, stack)
        if (this.onChangedListener != null) {
            this.onChangedListener!!.accept(slot)
        }
        this.owner.markNoUpdateSync()
    }

    @Nonnull
    override fun insertItem(slot: Int, @Nonnull stack: ItemStack, simulate: Boolean): ItemStack {
        if (stack.isEmpty) {
            return stack
        }
        val inserted = this.insertItemInternal(slot, stack, simulate)
        if (!simulate) {
            if (this.onChangedListener != null) {
                this.onChangedListener!!.accept(slot)
            }
            this.owner.markNoUpdateSync()
        }
        return inserted
    }

    @Nonnull
    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
        val extracted = super.extractItem(slot, amount, simulate)
        if (!simulate) {
            if (this.onChangedListener != null) {
                this.onChangedListener!!.accept(slot)
            }
            this.owner.markNoUpdateSync()
        }
        return extracted
    }

    fun writeNBT(): NBTTagCompound {
        val stackSet = ArrayList<ItemStack>()
        val stackSetIdxSet = IntArray(inventory.size)

        invSet@ for (i in inventory.indices) {
            val holder = this.inventory[i]
            val stackInHolder = holder.itemStack.get()
            if (stackInHolder.isEmpty) {
                stackSetIdxSet[i] = -1
                continue
            }

            for (stackSetIdx in stackSet.indices) {
                val stackInSet = stackSet[stackSetIdx]
                if (ItemUtils.matchStacks(stackInHolder, stackInSet)) {
                    stackSetIdxSet[i] = stackSetIdx
                    continue@invSet
                }
            }

            stackSet.add(stackInHolder)
            stackSetIdxSet[i] = stackSet.size - 1
        }

        val stackSetTag = NBTTagList()
        val invSetTag = NBTTagList()

        for (stack in stackSet) {
            val stackTag = stack.writeToNBT(NBTTagCompound())
            if (stack.count >= 127) {
                stackTag.setInteger("Count", stack.count)
            }
            stackSetTag.appendTag(stackTag)
        }
        for (setIdx in stackSetIdxSet) {
            invSetTag.appendTag(NBTTagByte(setIdx.toByte()))
        }

        val tag = NBTTagCompound()
        tag.setTag("stackSet", stackSetTag)
        tag.setTag("invSet", invSetTag)
        return tag
    }

    fun readNBT(tag: NBTTagCompound) {
        val stackSetTag = tag.getTagList("stackSet", Constants.NBT.TAG_COMPOUND)
        val invSetTag = tag.getTagList("invSet", Constants.NBT.TAG_BYTE)

        val stackSet = ArrayList<ItemStack?>()
        for (i in 0..<stackSetTag.tagCount()) {
            val stackTag = stackSetTag.getCompoundTagAt(i)
            val stack = ItemStack(stackTag)
            stack.setCount(stackTag.getInteger("Count"))
            stackSet.add(stack)
        }

        this.inventory = arrayOfNulls<SlotStackHolder>(invSetTag.tagCount())
        for (i in 0..<invSetTag.tagCount()) {
            val holder = SlotStackHolder(i)
            val setIdx = (invSetTag.get(i) as NBTPrimitive).getByte().toInt()
            if (setIdx != -1) {
                holder.itemStack.set(stackSet[setIdx]!!.copy())
            }
            this.inventory[i] = holder
        }

        updateInOutSlots()
        updateSlotLimits()

        if (onChangedListener != null) {
            onChangedListener!!.accept(-1)
        }
    }
}