package github.kasuminova.novaeng.mixin.mmce;

import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext.CraftingCheckResult;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = CraftingCheckResult.class, remap = false)
public abstract class MixinCraftingCheckResultAllocation {
    @Shadow
    @Final
    @Mutable
    private Object2IntMap<String> unlocErrorMessagesMap;

    /**
     * @author NovaEngineering
     * @reason Successful checks need no error map; allocate one only for the first actual error.
     */
    @Overwrite
    public void addError(final String error) {
        if (error.isEmpty()) return;
        if (unlocErrorMessagesMap == null) unlocErrorMessagesMap = new Object2IntOpenHashMap<>(2);
        ((Object2IntOpenHashMap<String>) unlocErrorMessagesMap).addTo(error, 1);
    }

    /**
     * @author NovaEngineering
     * @reason Preserve replacement semantics with lazy error storage.
     */
    @Overwrite
    public void overrideError(final String error) {
        if (unlocErrorMessagesMap != null) unlocErrorMessagesMap.clear();
        this.addError(error);
    }

    /**
     * @author NovaEngineering
     * @reason An unallocated map is the original empty error state.
     */
    @Overwrite
    public boolean isFailure() {
        return unlocErrorMessagesMap != null && !unlocErrorMessagesMap.isEmpty();
    }

    /**
     * @author NovaEngineering
     * @reason An unallocated map is the original successful state.
     */
    @Overwrite
    public boolean isSuccess() {
        return unlocErrorMessagesMap == null || unlocErrorMessagesMap.isEmpty();
    }

    /**
     * @author NovaEngineering
     * @reason Return an independent result list without boxing error frequencies.
     */
    @Overwrite
    public List<String> getUnlocalizedErrorMessages() {
        final ObjectArrayList<String> result = new ObjectArrayList<>(unlocErrorMessagesMap == null ? 0 : unlocErrorMessagesMap.size());
        if (unlocErrorMessagesMap != null) {
            result.addAll(unlocErrorMessagesMap.keySet());
            result.sort((left, right) -> Integer.compare(unlocErrorMessagesMap.getInt(left), unlocErrorMessagesMap.getInt(right)));
        }
        return result;
    }

    /**
     * @author NovaEngineering
     * @reason Select the first error directly instead of allocating and sorting two lists.
     */
    @Overwrite
    public String getFirstErrorMessage(final String defaultMessage) {
        if (unlocErrorMessagesMap == null || unlocErrorMessagesMap.isEmpty()) return defaultMessage;
        String first = defaultMessage;
        int minimum = 0;
        boolean found = false;
        final var entries = Object2IntMaps.fastIterator(unlocErrorMessagesMap);
        while (entries.hasNext()) {
            final var entry = entries.next();
            if (!found || entry.getIntValue() < minimum) {
                first = entry.getKey();
                minimum = entry.getIntValue();
                found = true;
            }
        }
        return first;
    }
}
