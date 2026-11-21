package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.performance.mmce.GenericItemHandlerCopy;
import hellfirepvp.modularmachinery.common.util.IItemHandlerImpl;
import hellfirepvp.modularmachinery.common.util.IItemHandlerImpl.SlotStackHolder;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = IItemHandlerImpl.class, remap = false)
public abstract class MixinItemHandlerGenericCopy implements GenericItemHandlerCopy {
    @Unique
    private static final SlotStackHolder[] nova$emptyInventory = new SlotStackHolder[0];
    @Shadow
    public boolean allowAnySlots;
    @Shadow
    public EnumFacing[] accessibleSides;
    @Shadow
    protected int[] slotLimits;
    @Shadow
    protected SlotStackHolder[] inventory;
    @Shadow
    protected int[] inSlots;
    @Shadow
    protected int[] outSlots;
    @Shadow
    protected int[] miscSlots;
    // copyFrom(MMCE handler) aliases the source's slot-index arrays. Never write into those arrays.
    @Unique
    private int[] nova$genericInSlots;
    @Unique
    private int[] nova$genericOutSlots;
    @Unique
    private boolean nova$genericCopy;

    @Override
    public IItemHandlerImpl nova$copyFromGeneric(final IItemHandlerModifiable source) {
        final int slots = source.getSlots();
        nova$genericCopy = true;
        if (nova$genericInSlots == null || nova$genericInSlots.length != slots) {
            nova$genericInSlots = new int[slots];
            nova$genericOutSlots = new int[slots];
            // These private index arrays have no mutable public accessor. Native
            // copyFrom replaces inSlots/outSlots, so it cannot invalidate this sequence.
            for (int i = 0; i < slots; i++) {
                nova$genericInSlots[i] = i;
                nova$genericOutSlots[i] = i;
            }
        }
        inSlots = nova$genericInSlots;
        outSlots = nova$genericOutSlots;
        miscSlots = IntArrays.EMPTY_ARRAY;
        allowAnySlots = false;
        accessibleSides = EnumFacing.VALUES;
        // The generic constructor deliberately uses the MMCE default limit, not source.getSlotLimit().
        if (slotLimits.length != 1) slotLimits = new int[1];
        slotLimits[0] = IItemHandlerImpl.DEFAULT_SLOT_LIMIT;
        if (inventory.length != slots) {
            inventory = new SlotStackHolder[slots];
        }
        for (int i = 0; i < slots; i++) {
            SlotStackHolder holder = inventory[i];
            if (holder == null) inventory[i] = holder = new SlotStackHolder(i);
            final ItemStack stack = source.getStackInSlot(i);
            holder.itemStack.set(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        return (IItemHandlerImpl) (Object) this;
    }

    @Override
    public void nova$releaseGenericContents() {
        if (!nova$genericCopy) return;
        // MMCE's session contract ends ownership here. Idle shells must not pin copied NBT graphs.
        for (final SlotStackHolder holder : inventory) if (holder != null) holder.itemStack.set(ItemStack.EMPTY);
        inSlots = IntArrays.EMPTY_ARRAY;
        outSlots = IntArrays.EMPTY_ARRAY;
        miscSlots = IntArrays.EMPTY_ARRAY;
        if (inventory.length > 256) {
            inventory = nova$emptyInventory;
            nova$genericInSlots = null;
            nova$genericOutSlots = null;
        }
        nova$genericCopy = false;
    }
}
