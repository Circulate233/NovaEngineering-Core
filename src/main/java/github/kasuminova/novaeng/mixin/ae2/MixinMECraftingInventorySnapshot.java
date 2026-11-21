package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.crafting.MECraftingInventory;
import github.kasuminova.novaeng.common.performance.ae2.CraftingSnapshotCopy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = MECraftingInventory.class, remap = false)
public abstract class MixinMECraftingInventorySnapshot {
    @Shadow
    @Final
    private IItemList<IAEItemStack> localCache;

    // Do not redirect iterator(): RandomComplement already redirects that invocation.
    // The argument is first loaded only after the owned localCache has been assigned.
    @ModifyVariable(method = "<init>(Lappeng/api/storage/data/IItemList;)V",
        at = @At(value = "LOAD", ordinal = 0), argsOnly = true, ordinal = 0, require = 1, allow = 1)
    private IItemList<IAEItemStack> nova$copySnapshot(final IItemList<IAEItemStack> source) {
        return CraftingSnapshotCopy.copyOrOriginal(source, localCache);
    }
}
