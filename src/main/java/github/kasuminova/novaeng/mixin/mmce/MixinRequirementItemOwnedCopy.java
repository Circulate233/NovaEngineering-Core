package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.performance.mmce.OwnedRequirementStack;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementItem;
import hellfirepvp.modularmachinery.common.machine.IOType;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RequirementItem.class, remap = false)
public abstract class MixinRequirementItemOwnedCopy {
    @Redirect(method = "deepCopyModified(Ljava/util/List;)Lhellfirepvp/modularmachinery/common/crafting/requirement/RequirementItem;",
        at = @At(value = "NEW", target = "(Lhellfirepvp/modularmachinery/common/machine/IOType;Lnet/minecraft/item/ItemStack;)Lhellfirepvp/modularmachinery/common/crafting/requirement/RequirementItem;"), require = 1)
    private RequirementItem nova$takeOwnedStack(final IOType io, final ItemStack owned) {
        // A tiny untagged copy can cost less than setting up a dynamic ownership scope.
        if (!owned.hasTagCompound()) return new RequirementItem(io, owned);
        return OwnedRequirementStack.construct(owned, () -> new RequirementItem(io, owned));
    }

    @Redirect(method = "<init>(Lhellfirepvp/modularmachinery/common/machine/IOType;Lnet/minecraft/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;copy()Lnet/minecraft/item/ItemStack;", remap = true), require = 1)
    private ItemStack nova$copyUnlessOwned(final ItemStack source) {
        return OwnedRequirementStack.copyForConstructor(source);
    }
}
