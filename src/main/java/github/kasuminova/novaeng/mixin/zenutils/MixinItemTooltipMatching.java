package github.kasuminova.novaeng.mixin.zenutils;

import com.llamalad7.mixinextras.sugar.Local;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.mc1120.item.MCItemStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import youyihj.zenutils.api.item.ItemTooltipModification;

import java.util.Map;

@Mixin(value = ItemTooltipModification.TooltipEventHandler.class, remap = false)
public class MixinItemTooltipMatching {
    @Redirect(method = "onItemTooltip", at = @At(value = "INVOKE",
        target = "Lcrafttweaker/api/minecraft/CraftTweakerMC;getIItemStackForMatching(Lnet/minecraft/item/ItemStack;)Lcrafttweaker/api/item/IItemStack;"),
        require = 1)
    private static IItemStack nova$skipUnrelatedTooltipWrapper(final ItemStack item,
                                                               @Local(name = "entry") final Map.Entry<IIngredient, ?> entry) {
        final IIngredient selector = entry.getKey();
        if (selector != null && selector.getClass() == MCItemStack.class
            && selector.getInternal() instanceof ItemStack target && !target.hasTagCompound()
            && item != null && target.getItem() != item.getItem()) {
            // Exact built-in MCItemStack.matches rejects a different item before any
            // metadata/size checks, and rejects null immediately. A tag-bearing exact
            // selector can enter matchesExact first, so it deliberately keeps fallback.
            return null;
        }
        // In particular, keep a fresh mutable wrapper for every custom matcher and
        // every potentially matching entry: predicates/modifiers can mutate the item.
        return CraftTweakerMC.getIItemStackForMatching(item);
    }
}
