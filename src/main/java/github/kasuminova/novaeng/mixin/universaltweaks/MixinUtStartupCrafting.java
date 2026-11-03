package github.kasuminova.novaeng.mixin.universaltweaks;

import github.kasuminova.novaeng.common.performance.StartupCraftingScope;
import mod.acgaming.universaltweaks.tweaks.performance.craftingcache.UTCraftingCache;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = UTCraftingCache.class, remap = false)
public abstract class MixinUtStartupCrafting {
    /**
     * @author circulation
     * @reason Reuse ordered candidates in startup batches, preserving the original fallback outside them.
     */
    @Overwrite
    public static IRecipe findMatchingRecipeDefault(final InventoryCrafting matrix, final World world) {
        return StartupCraftingScope.findMatchingRecipe(matrix, world);
    }
}
