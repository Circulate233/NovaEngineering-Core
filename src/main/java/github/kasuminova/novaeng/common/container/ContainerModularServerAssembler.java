package github.kasuminova.novaeng.common.container;

import github.kasuminova.novaeng.common.container.slot.AssemblySlotManager;
import github.kasuminova.novaeng.common.container.slot.SlotModularServer;
import github.kasuminova.novaeng.common.tile.TileModularServerAssembler;
import hellfirepvp.modularmachinery.common.container.ContainerBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerModularServerAssembler extends ContainerBase<TileModularServerAssembler> {

    protected final EntityPlayer opening;
    protected final SlotModularServer slotModularServer;
    @Nullable
    private AssemblySlotManager slotManager;

    public ContainerModularServerAssembler(final TileModularServerAssembler owner, final EntityPlayer opening) {
        super(owner, opening);
        this.opening = opening;
        final var server = owner.getServer();
        this.slotManager = server == null ? null : server.getSlotManager();
        if (this.slotManager != null) {
            this.slotManager.addAllSlotToContainer(this);
        }
        this.slotModularServer = new SlotModularServer(owner.getServerInventory().asGUIAccess(), 0, 302, 126);
        this.addSlotToContainer(this.slotModularServer);
        owner.addContainer(this);
    }

    @Nullable
    public AssemblySlotManager getSlotManager() {
        return this.slotManager;
    }

    public void setSlotManager(@Nullable final AssemblySlotManager manager) {
        this.slotManager = manager;
    }

    @Override
    public boolean canInteractWith(final EntityPlayer player) {
        return true;
    }

    @Override
    public void onContainerClosed(@Nonnull final EntityPlayer player) {
        super.onContainerClosed(player);
        this.owner.removeContainer(this);
    }

    public void reInitSlots() {
        this.inventorySlots.clear();
        this.inventoryItemStacks.clear();
        this.addPlayerSlots(this.opening);
        final var server = this.owner.getServer();
        this.slotManager = server == null ? null : server.getSlotManager();
        if (this.slotManager != null) {
            this.slotManager.addAllSlotToContainer(this);
        }
        this.addSlotToContainer(this.slotModularServer);
    }

    @Override
    @Nonnull
    public Slot addSlotToContainer(@Nonnull final Slot slot) {
        return super.addSlotToContainer(slot);
    }

    @Override
    @Nonnull
    public ItemStack transferStackInSlot(@Nonnull final EntityPlayer player, final int index) {
        final Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getStack();
        final ItemStack original = stack.copy();
        if (index < 27) {
            if (!this.mergeItemStack(stack, 27, 36, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 36) {
            if (!this.mergeItemStack(stack, 0, 27, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.mergeItemStack(stack, 0, 36, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.getCount() == 0) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    protected void addPlayerSlots(final EntityPlayer opening) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(opening.inventory, j + i * 9 + 9, 133 + j * 18, 124 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlotToContainer(new Slot(opening.inventory, i, 133 + i * 18, 182));
        }
    }
}
