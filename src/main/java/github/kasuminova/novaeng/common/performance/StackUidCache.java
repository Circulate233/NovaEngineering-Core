package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.runtime.SubtypeRegistry;
import mezz.jei.startup.StackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Keeps HEI's identity cache; only shares UIDs whose inputs are known to be item/metadata alone.
 */
public final class StackUidCache extends Reference2ObjectOpenHashMap<ItemStack, String> {
    private final StackHelper.UidMode mode;
    private final ISubtypeRegistry subtypes;
    private final Reference2ObjectOpenHashMap<Item, Long2ObjectOpenHashMap<String>> plain = new Reference2ObjectOpenHashMap<>();

    public StackUidCache(final StackHelper.UidMode mode, final ISubtypeRegistry subtypes) {
        this.mode = mode;
        this.subtypes = subtypes;
    }

    @Override
    public String get(final Object key) {
        final String existing = super.get(key);
        if (existing != null || !(key instanceof ItemStack stack) || !canShare(stack)) {
            return existing;
        }
        final Long2ObjectOpenHashMap<String> metadata = plain.get(stack.getItem());
        final String shared = metadata == null ? null : metadata.get(metadataKey(stack));
        if (shared != null) {
            super.put(stack, shared);
        }
        return shared;
    }

    @Override
    public String put(final ItemStack stack, final String value) {
        final String old = super.put(stack, value);
        if (canShare(stack)) {
            plain.computeIfAbsent(stack.getItem(), item -> new Long2ObjectOpenHashMap<>())
                 .put(metadataKey(stack), value);
        }
        return old;
    }

    @Override
    public void clear() {
        super.clear();
        plain.clear();
    }

    private boolean canShare(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (mode == StackHelper.UidMode.WILDCARD || stack.getMetadata() == 32767) {
            return true;
        }
        // FULL serializes ForgeCaps. Custom subtype interpreters may also inspect capabilities/count/world state.
        return mode == StackHelper.UidMode.NORMAL && subtypes.getClass() == SubtypeRegistry.class
            && !subtypes.hasSubtypeInterpreter(stack);
    }

    private long metadataKey(final ItemStack stack) {
        return mode == StackHelper.UidMode.WILDCARD || stack.getMetadata() == 32767
            ? Long.MIN_VALUE : stack.getHasSubtypes() ? stack.getMetadata() : 0;
    }
}
