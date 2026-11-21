package github.kasuminova.novaeng.mixin.mmce;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(value = CraftingCheckResult.class, remap = false)
public abstract class MixinCraftingCheckResultAllocation {
    @Shadow
    @Final
    @Mutable
    private Object2IntMap<String> unlocErrorMessagesMap;

    @Unique
    private String nova$onlyError;
    @Unique
    private int nova$onlyErrorCount;

    // The development jar initializes this eagerly; the deployed jar already initializes it lazily.
    @WrapOperation(method = "<init>*", at = @At(value = "NEW", target =
        "it/unimi/dsi/fastutil/objects/Object2IntOpenHashMap"), require = 0)
    private static Object2IntOpenHashMap<String> nova$deferErrors(final Operation<Object2IntOpenHashMap<String>> original) {
        return null;
    }

    /**
     * @author NovaEngineering
     * @reason Keep the common single-error result inline; preserve the original hash order on promotion.
     */
    @Overwrite
    public void addError(final String error) {
        if (error.isEmpty()) return;
        if (unlocErrorMessagesMap == null) {
            if (nova$onlyError == null) {
                nova$onlyError = error;
                nova$onlyErrorCount = 1;
                return;
            }
            if (nova$onlyError.equals(error)) {
                nova$onlyErrorCount++;
                return;
            }
            // Capacity affects iteration order and therefore the winner among equal-frequency errors.
            unlocErrorMessagesMap = new Object2IntOpenHashMap<>();
            unlocErrorMessagesMap.put(nova$onlyError, nova$onlyErrorCount);
            nova$onlyError = null;
            nova$onlyErrorCount = 0;
        }
        ((Object2IntOpenHashMap<String>) unlocErrorMessagesMap).addTo(error, 1);
    }

    /**
     * @author NovaEngineering
     * @reason Preserve replacement semantics with lazy error storage.
     */
    @Overwrite
    public void overrideError(final String error) {
        if (unlocErrorMessagesMap != null) unlocErrorMessagesMap.clear();
        nova$onlyError = null;
        nova$onlyErrorCount = 0;
        this.addError(error);
    }

    /**
     * @author NovaEngineering
     * @reason An unallocated map is the original empty error state.
     */
    @Overwrite
    public boolean isFailure() {
        return nova$onlyError != null || (unlocErrorMessagesMap != null && !unlocErrorMessagesMap.isEmpty());
    }

    /**
     * @author NovaEngineering
     * @reason An unallocated map is the original successful state.
     */
    @Overwrite
    public boolean isSuccess() {
        return !isFailure();
    }

    /**
     * @author NovaEngineering
     * @reason Return an independent result list without boxing error frequencies.
     */
    @Overwrite
    public List<String> getUnlocalizedErrorMessages() {
        final ObjectArrayList<String> result = new ObjectArrayList<>(
            nova$onlyError != null ? 1 : unlocErrorMessagesMap == null ? 0 : unlocErrorMessagesMap.size());
        if (nova$onlyError != null) result.add(nova$onlyError);
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
        if (nova$onlyError != null) return nova$onlyError;
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
