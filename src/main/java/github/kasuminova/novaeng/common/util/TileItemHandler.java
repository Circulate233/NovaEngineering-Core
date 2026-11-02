package github.kasuminova.novaeng.common.util;

import github.kasuminova.novaeng.common.performance.ItemStackIndex;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntitySynchronized;
import hellfirepvp.modularmachinery.common.util.IItemHandlerImpl;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class TileItemHandler extends IItemHandlerImpl {

    protected final BitSet availableSlots = new BitSet();
    private final TileEntitySynchronized owner;
    private final String invName;
    @Nullable
    private IntConsumer onChangedListener;

    public TileItemHandler(final TileEntitySynchronized owner, final int[] inSlots,
                           final int[] outSlots, final String invName) {
        super(inSlots, outSlots);
        this.owner = owner;
        this.invName = invName;
    }

    public static TileItemHandler create(final TileEntitySynchronized owner, final int slotCount, final String invName) {
        final int[] slots = slotIds(slotCount);
        return new TileItemHandler(owner, slots, slots, invName).setAllSlotAvailable().updateSlotLimits();
    }

    private static int[] slotIds(final int count) {
        final int[] slots = new int[count];
        for (int i = 0; i < count; i++) {
            slots[i] = i;
        }
        return slots;
    }

    public TileEntitySynchronized getOwner() {
        return this.owner;
    }

    public String getInvName() {
        return this.invName;
    }

    public TileItemHandler updateInOutSlots() {
        final int[] slots = slotIds(this.inventory.length);
        this.inSlots = slots;
        this.outSlots = slots;
        return this;
    }

    public TileItemHandler updateSlotLimits() {
        this.slotLimits = new int[this.inventory.length];
        Arrays.fill(this.slotLimits, 1);
        return this;
    }

    public TileItemHandler setOnChangedListener(@Nullable final IntConsumer listener) {
        this.onChangedListener = listener;
        return this;
    }

    public boolean isSlotAvailable(final int slot) {
        return this.availableSlots.get(slot);
    }

    public TileItemHandler setSlotAvailable(final int slot) {
        this.availableSlots.set(slot);
        return this;
    }

    public TileItemHandler setAllSlotAvailable() {
        this.availableSlots.set(0, this.inventory.length);
        return this;
    }

    public TileItemHandler setUnavailableSlots(final int[] slots) {
        for (final int slot : slots) {
            this.availableSlots.clear(slot);
        }
        return this;
    }

    public TileItemHandler setUnavailableSlot(final int slot) {
        this.availableSlots.clear(slot);
        return this;
    }

    public IntStream getAvailableSlotsStream() {
        return this.availableSlots.stream();
    }

    private void changed(final int slot) {
        if (this.onChangedListener != null) {
            this.onChangedListener.accept(slot);
        }
        this.owner.markNoUpdateSync();
    }

    @Override
    public void setStackInSlot(final int slot, @Nonnull final ItemStack stack) {
        super.setStackInSlot(slot, stack);
        this.changed(slot);
    }

    @Override
    @Nonnull
    public ItemStack insertItem(final int slot, @Nonnull final ItemStack stack, final boolean simulate) {
        if (stack.isEmpty()) {
            return stack;
        }
        final ItemStack inserted = this.insertItemInternal(slot, stack, simulate);
        if (!simulate) {
            this.changed(slot);
        }
        return inserted;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        final ItemStack extracted = super.extractItem(slot, amount, simulate);
        if (!simulate) {
            this.changed(slot);
        }
        return extracted;
    }

    public NBTTagCompound writeNBT() {
        final List<ItemStack> stacks = new ObjectArrayList<>();
        final int[] indexes = new int[this.inventory.length];
        if (!this.shouldIndexStacks()) {
            inventoryLoop:
            for (int i = 0; i < this.inventory.length; i++) {
                final ItemStack stack = this.inventory[i].itemStack.get();
                if (stack.isEmpty()) {
                    indexes[i] = -1;
                    continue;
                }
                for (int j = 0; j < stacks.size(); j++) {
                    if (ItemUtils.matchStacks(stack, stacks.get(j))) {
                        indexes[i] = j;
                        continue inventoryLoop;
                    }
                }
                indexes[i] = stacks.size();
                stacks.add(stack);
            }
        } else {
            try (final ItemStackIndex stackIndex = ItemStackIndex.open(stacks, false)) {
                for (int i = 0; i < this.inventory.length; i++) {
                    final ItemStack stack = this.inventory[i].itemStack.get();
                    indexes[i] = stack.isEmpty() ? -1 : stackIndex.findOrAdd(stack);
                }
            }
        }
        final NBTTagList stackSet = new NBTTagList();
        final NBTTagList invSet = new NBTTagList();
        for (final ItemStack stack : stacks) {
            final NBTTagCompound tag = stack.writeToNBT(new NBTTagCompound());
            if (stack.getCount() >= 127) {
                tag.setInteger("Count", stack.getCount());
            }
            stackSet.appendTag(tag);
        }
        for (final int index : indexes) {
            invSet.appendTag(new NBTTagByte((byte) index));
        }
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("stackSet", stackSet);
        tag.setTag("invSet", invSet);
        return tag;
    }

    private boolean shouldIndexStacks() {
        if (this.inventory.length < ItemStackIndex.INDEX_THRESHOLD) {
            return false;
        }
        Item first = null;
        int checked = 0;
        for (final var holder : this.inventory) {
            final ItemStack stack = holder.itemStack.get();
            if (!stack.isEmpty()) {
                if (first == null) {
                    first = stack.getItem();
                } else if (first != stack.getItem()) {
                    return true;
                }
                if (++checked == ItemStackIndex.INDEX_THRESHOLD) {
                    return false;
                }
            }
        }
        return false;
    }

    public void readNBT(final NBTTagCompound tag) {
        final NBTTagList stackSet = tag.getTagList("stackSet", Constants.NBT.TAG_COMPOUND);
        final NBTTagList invSet = tag.getTagList("invSet", Constants.NBT.TAG_BYTE);
        final List<ItemStack> stacks = new ObjectArrayList<>(stackSet.tagCount());
        for (int i = 0; i < stackSet.tagCount(); i++) {
            final NBTTagCompound stackTag = stackSet.getCompoundTagAt(i);
            final ItemStack stack = new ItemStack(stackTag);
            stack.setCount(stackTag.getInteger("Count"));
            stacks.add(stack);
        }
        this.inventory = new SlotStackHolder[invSet.tagCount()];
        for (int i = 0; i < this.inventory.length; i++) {
            final SlotStackHolder holder = new SlotStackHolder(i);
            final int index = ((NBTPrimitive) invSet.get(i)).getByte();
            if (index != -1) {
                holder.itemStack.set(stacks.get(index).copy());
            }
            this.inventory[i] = holder;
        }
        this.updateInOutSlots();
        this.updateSlotLimits();
        if (this.onChangedListener != null) {
            this.onChangedListener.accept(-1);
        }
    }
}
