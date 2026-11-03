package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.crafting.MECraftingInventory;
import github.kasuminova.novaeng.common.performance.AvailableStackIteration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MECraftingInventory.class, remap = false)
public abstract class MixinMECraftingInventoryIteration {
    @Shadow
    @Final
    private IItemList<IAEItemStack> localCache;

    /**
     * @author NovaEngineering
     * @reason Scan owned records without allocating two iterators per item variant; output still owns its copies.
     */
    @Overwrite
    @SuppressWarnings("unchecked")
    public IItemList<IAEItemStack> getAvailableItems(final IItemList<IAEItemStack> out) {
        if (localCache instanceof AvailableStackIteration<?> iteration) {
            ((AvailableStackIteration<IAEItemStack>) iteration).nova$forEachAvailable(out::add);
        } else {
            for (final IAEItemStack stack : localCache) out.add(stack);
        }
        return out;
    }
}
