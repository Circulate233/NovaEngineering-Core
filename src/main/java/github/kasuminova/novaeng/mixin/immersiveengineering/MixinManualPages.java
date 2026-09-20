package github.kasuminova.novaeng.mixin.immersiveengineering;

import blusunrize.lib.manual.ManualPages;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.registry.RegistryNamespaced;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.Iterator;

/**
 * Removes the handbook crafting-page recipe scan.
 *
 * <p>Both page types rebuild their display tables by walking the entire Forge recipe registry once per
 * page. The players of this pack never consult those pages, so the scan and the resulting display are
 * erased entirely. Only the registry walk is replaced; clearing the tables and rebuilding the provided
 * item list still run, so item links and page navigation keep working.</p>
 */
public final class MixinManualPages {

    private MixinManualPages() {
    }

    @Mixin(value = ManualPages.Crafting.class, remap = false)
    public abstract static class Crafting {

        @Redirect(method = "recalculateCraftingRecipes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/registry/RegistryNamespaced;iterator()Ljava/util/Iterator;",
            remap = true), remap = false, require = 1)
        private Iterator<IRecipe> nova$skipRecipeScan(final RegistryNamespaced<?, IRecipe> registry) {
            return Collections.emptyIterator();
        }
    }

    @Mixin(value = ManualPages.CraftingMulti.class, remap = false)
    public abstract static class CraftingMulti {

        @Redirect(method = "recalculateCraftingRecipes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/registry/RegistryNamespaced;iterator()Ljava/util/Iterator;",
            remap = true), remap = false, require = 1)
        private Iterator<IRecipe> nova$skipRecipeScan(final RegistryNamespaced<?, IRecipe> registry) {
            return Collections.emptyIterator();
        }
    }
}
