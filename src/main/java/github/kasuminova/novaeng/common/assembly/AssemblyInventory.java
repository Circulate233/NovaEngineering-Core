package github.kasuminova.novaeng.common.assembly;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;

import java.util.AbstractList;

/**
 * A reusable view of the player's inventory and one level of inventory items.
 */
final class AssemblyInventory extends AbstractList<ItemStack> {
    private final PlayerInvWrapper root;
    private final ReferenceArrayList<IItemHandler> handlers = new ReferenceArrayList<>();
    private final ReferenceArrayList<ItemStack> owners = new ReferenceArrayList<>();
    private final IntArrayList ownerSlots = new IntArrayList();
    private final IntArrayList starts = new IntArrayList();
    private int size;

    AssemblyInventory(final EntityPlayerMP player) {
        root = new PlayerInvWrapper(player.inventory);
        refresh();
    }

    void refresh() {
        handlers.clear();
        owners.clear();
        ownerSlots.clear();
        starts.clear();
        size = root.getSlots();
        for (int slot = 0; slot < root.getSlots(); slot++) {
            final var stack = root.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            final var handler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler == null || handler == root || handlers.contains(handler)) continue;
            starts.add(size);
            handlers.add(handler);
            owners.add(stack);
            ownerSlots.add(slot);
            size += handler.getSlots();
        }
    }

    private int source(final int index) {
        for (int i = starts.size() - 1; i >= 0; i--) if (index >= starts.getInt(i)) return i;
        return -1;
    }

    private boolean current(final int source) {
        final var stack = root.getStackInSlot(ownerSlots.getInt(source));
        return !stack.isEmpty() && stack == owners.get(source)
            && stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) == handlers.get(source);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public ItemStack get(final int index) {
        if (index < root.getSlots()) return root.getStackInSlot(index);
        final int source = source(index);
        return source < 0 || !current(source) ? ItemStack.EMPTY
            : handlers.get(source).getStackInSlot(index - starts.getInt(source));
    }

    boolean writable(final int index) {
        if (index < root.getSlots()) return true;
        final int source = source(index);
        return source >= 0 && current(source) && handlers.get(source) instanceof IItemHandlerModifiable;
    }

    ItemStack extract(final int index, final int amount, final boolean simulate) {
        if (index < root.getSlots()) return root.extractItem(index, amount, simulate);
        final int source = source(index);
        return source < 0 || !current(source) ? ItemStack.EMPTY
            : handlers.get(source).extractItem(index - starts.getInt(source), amount, simulate);
    }

    ItemStack insert(final int index, final ItemStack stack) {
        if (index < root.getSlots()) return root.insertItem(index, stack, false);
        final int source = source(index);
        return source < 0 || !current(source) ? stack
            : handlers.get(source).insertItem(index - starts.getInt(source), stack, false);
    }

    @Override
    public ItemStack set(final int index, final ItemStack stack) {
        final var previous = get(index);
        if (index < root.getSlots()) root.setStackInSlot(index, stack);
        else {
            final int source = source(index);
            if (source < 0 || !current(source) || !(handlers.get(source) instanceof IItemHandlerModifiable writable))
                throw new IllegalStateException("Construction inventory is no longer writable");
            writable.setStackInSlot(index - starts.getInt(source), stack);
        }
        return previous;
    }
}
