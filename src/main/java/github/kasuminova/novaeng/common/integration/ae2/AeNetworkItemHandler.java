package github.kasuminova.novaeng.common.integration.ae2;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEItemKey;
import ae2.api.storage.MEStorage;
import github.kasuminova.novaeng.common.performance.PerformanceMetrics;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class AeNetworkItemHandler implements IItemHandler {

    private static final int SLOTS = 1;
    private static final int SLOT_LIMIT = 64;

    private final MEStorage storage;
    private final IActionSource source;
    private ItemStack lastDefinition;
    private AEItemKey lastKey;

    public AeNetworkItemHandler(final MEStorage storage, final IActionSource source) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public int getSlots() {
        return SLOTS;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(final int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(final int slot, final @NotNull ItemStack stack, final boolean simulate) {
        if (slot != 0 || stack.isEmpty()) {
            return stack;
        }

        final AEItemKey key = this.keyOf(stack);
        if (key == null) {
            return stack;
        }

        PerformanceMetrics.add(PerformanceMetrics.Counter.AE_OUTPUT_INSERTS, 1);
        final long inserted = this.storage.insert(key, stack.getCount(),
            simulate ? Actionable.SIMULATE : Actionable.MODULATE, this.source);
        if (inserted <= 0) {
            return stack;
        }
        if (inserted >= stack.getCount()) {
            return ItemStack.EMPTY;
        }

        final ItemStack remainder = stack.copy();
        remainder.setCount(stack.getCount() - (int) inserted);
        return remainder;
    }

    @Override
    public @NotNull ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(final int slot) {
        return SLOT_LIMIT;
    }

    @Override
    public boolean isItemValid(final int slot, final @NotNull ItemStack stack) {
        return slot == 0 && !stack.isEmpty();
    }

    private AEItemKey keyOf(final ItemStack stack) {
        final ItemStack last = this.lastDefinition;
        if (last != null && ItemStack.areItemsEqual(last, stack) && ItemStack.areItemStackTagsEqual(last, stack)) {
            return this.lastKey;
        }

        final AEItemKey key = AEItemKey.of(stack);
        if (key != null) {
            final ItemStack definition = stack.copy();
            definition.setCount(1);
            this.lastDefinition = definition;
            this.lastKey = key;
        }
        return key;
    }

}
