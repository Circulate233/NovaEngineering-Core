package github.kasuminova.novaeng.mixin.cofh;

import cofh.core.util.helpers.ItemHelper;
import github.kasuminova.novaeng.common.performance.StartupCraftingScope;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemHelper.class, remap = false)
public abstract class MixinCoFHStartupCrafting {
    @Redirect(method = "getCraftingResult", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/item/crafting/CraftingManager;findMatchingResult(Lnet/minecraft/inventory/InventoryCrafting;Lnet/minecraft/world/World;)Lnet/minecraft/item/ItemStack;",
        remap = true), require = 1)
    private static ItemStack nova$startupCandidates(final InventoryCrafting craftMatrix, final World worldIn) {
        return StartupCraftingScope.findMatchingResult(craftMatrix, worldIn);
    }
}
