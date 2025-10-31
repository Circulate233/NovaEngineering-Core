package github.kasuminova.novaeng.common.container

import github.kasuminova.novaeng.common.container.slot.AssemblySlotManager
import github.kasuminova.novaeng.common.container.slot.SlotModularServer
import github.kasuminova.novaeng.common.tile.TileModularServerAssembler
import hellfirepvp.modularmachinery.common.container.ContainerBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import javax.annotation.Nonnull

open class ContainerModularServerAssembler(owner: TileModularServerAssembler, protected val opening: EntityPlayer) :
    ContainerBase<TileModularServerAssembler>(
        owner,
        opening
    ) {
    protected val slotModularServer: SlotModularServer

    var slotManager: AssemblySlotManager?

    init {
        val server = owner.getServer()
        this.slotManager = server?.getSlotManager()
        if (this.slotManager != null) {
            this.slotManager!!.addAllSlotToContainer(this)
        }

        slotModularServer = SlotModularServer(owner.getServerInventory().asGUIAccess(), 0, 302, 126)
        this.addSlotToContainer(slotModularServer)
        this.owner!!.addContainer(this)
    }

    override fun onContainerClosed(@Nonnull playerIn: EntityPlayer) {
        super.onContainerClosed(playerIn)
        this.owner!!.removeContainer(this)
    }

    fun reInitSlots() {
        this.inventorySlots.clear()
        this.inventoryItemStacks.clear()
        this.addPlayerSlots(opening)

        val server = owner!!.getServer()
        this.slotManager = server?.getSlotManager()
        if (this.slotManager != null) {
            this.slotManager!!.addAllSlotToContainer(this)
        }

        this.addSlotToContainer(slotModularServer)
    }

    @Nonnull
    public override fun addSlotToContainer(@Nonnull slotIn: Slot): Slot {
        return super.addSlotToContainer(slotIn)
    }

    @Nonnull
    override fun transferStackInSlot(@Nonnull playerIn: EntityPlayer, index: Int): ItemStack {
        var itemstack = ItemStack.EMPTY
        val slot = this.inventorySlots[index]

        if (slot != null && slot.hasStack) {
            val stackInSlot = slot.stack
            itemstack = stackInSlot.copy()

            if (index < 36) {
//                if (!itemstack1.isEmpty() && itemstack1.getItem() instanceof ItemBlueprint) {
//                    Slot sb = this.inventorySlots.get(this.slotBlueprint.slotNumber);
//                    if (!sb.getHasStack()) {
//                        if (!this.mergeItemStack(itemstack1, sb.slotNumber, sb.slotNumber + 1, false)) {
//                            return ItemStack.EMPTY;
//                        }
//                    }
//                }
            }

            if (index < 27) {
                if (!this.mergeItemStack(stackInSlot, 27, 36, false)) {
                    return ItemStack.EMPTY
                }
            } else if (index < 36) {
                if (!this.mergeItemStack(stackInSlot, 0, 27, false)) {
                    return ItemStack.EMPTY
                }
            } else if (!this.mergeItemStack(stackInSlot, 0, 36, false)) {
                return ItemStack.EMPTY
            }

            if (stackInSlot.count == 0) {
                slot.putStack(ItemStack.EMPTY)
            } else {
                slot.onSlotChanged()
            }

            if (stackInSlot.count == itemstack.count) {
                return ItemStack.EMPTY
            }

            slot.onTake(playerIn, stackInSlot)
        }

        return itemstack
    }

    override fun addPlayerSlots(opening: EntityPlayer) {
        for (i in 0..2) {
            for (j in 0..8) {
                addSlotToContainer(Slot(opening.inventory, j + i * 9 + 9, 133 + j * 18, 124 + i * 18))
            }
        }
        for (i in 0..8) {
            addSlotToContainer(Slot(opening.inventory, i, 133 + i * 18, 182))
        }
    }
}
