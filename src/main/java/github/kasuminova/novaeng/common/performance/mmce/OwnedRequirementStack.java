package github.kasuminova.novaeng.common.performance.mmce;

import net.minecraft.item.ItemStack;

/** Transfers a freshly copied stack only to the constructor receiving that exact stack. */
public final class OwnedRequirementStack {
    private static final ScopedValue<ItemStack> OWNED = ScopedValue.newInstance();

    private OwnedRequirementStack() {
    }

    public static <T> T construct(final ItemStack owned, final ScopedValue.CallableOp<T, RuntimeException> constructor) {
        return ScopedValue.where(OWNED, owned).call(constructor);
    }

    public static ItemStack copyForConstructor(final ItemStack stack) {
        if (OWNED.isBound() && OWNED.get() == stack) {
            // ItemStack.copy returns the EMPTY singleton after a modifier reduces count to zero.
            return stack.isEmpty() ? ItemStack.EMPTY : stack;
        }
        return stack.copy();
    }
}
