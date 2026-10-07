package github.kasuminova.novaeng.common.performance;

import appeng.util.item.AEItemStack;
import net.minecraft.item.ItemStack;

import java.util.Objects;

/** Reuses item conversion only. Network, permission, slot and notification code still runs for each insert. */
public final class AeOutputPreparation {
    private static final ThreadLocal<OperationCache<ItemStack, AEItemStack>> SCOPES = new ThreadLocal<>();

    private AeOutputPreparation() {
    }

    public static void enter() {
        OperationCache<ItemStack, AEItemStack> cache = SCOPES.get();
        if (cache == null) {
            cache = new OperationCache<>();
            SCOPES.set(cache);
        }
        cache.enter();
    }

    public static void leave() {
        SCOPES.get().leave();
    }

    public static AEItemStack convert(final ItemStack stack) {
        PerformanceMetrics.add(PerformanceMetrics.Counter.AE_OUTPUT_INSERTS, 1);
        final OperationCache<ItemStack, AEItemStack> cache = SCOPES.get();
        if (cache == null || !cache.active() || stack.isEmpty()) {
            return AEItemStack.fromItemStack(stack);
        }
        AEItemStack template = cache.get(stack);
        if (template != null) {
            final ItemStack definition = template.getDefinition();
            if (!ItemStack.areItemsEqual(definition, stack) || !ItemStack.areItemStackTagsEqual(definition, stack)
                || !definition.areCapsCompatible(stack)) {
                template = null;
            }
        }
        if (template == null) {
            template = AEItemStack.fromItemStack(stack);
            cache.put(stack, template);
        }
        // injectItems may mutate its input. The cached canonical definition never escapes.
        final AEItemStack converted = (AEItemStack) Objects.requireNonNull(template).copy();
        converted.setStackSize(stack.getCount());
        return converted;
    }
}
