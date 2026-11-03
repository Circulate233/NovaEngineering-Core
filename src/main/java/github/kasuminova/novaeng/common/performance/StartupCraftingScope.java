package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.common.util.RegistryIdLookupAccess;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Candidate reuse lasts only for one initialization call; registry and ore changes invalidate it.
 */
public final class StartupCraftingScope implements AutoCloseable {
    private static final ThreadLocal<StartupCraftingScope> ACTIVE = new ThreadLocal<>();
    private static final AtomicLong INGREDIENT_REVISION = new AtomicLong();
    private final StartupCraftingScope previous;
    private CraftingCandidateIndex index;
    private long recipeRevision = -1;
    private long itemRevision = -1;
    private long ingredientRevision = -1;

    private StartupCraftingScope(final StartupCraftingScope previous) {
        this.previous = previous;
    }

    public static StartupCraftingScope open() {
        final StartupCraftingScope scope = new StartupCraftingScope(ACTIVE.get());
        ACTIVE.set(scope);
        return scope;
    }

    public static void ingredientsChanged() {
        INGREDIENT_REVISION.incrementAndGet();
    }

    public static IRecipe findMatchingRecipe(final InventoryCrafting matrix, final World world) {
        final StartupCraftingScope scope = ACTIVE.get();
        if (scope != null) {
            final CraftingCandidateIndex index = scope.index();
            if (index != null) return index.find(matrix, world);
        }
        for (final IRecipe recipe : CraftingManager.REGISTRY) {
            if (recipe.matches(matrix, world)) return recipe;
        }
        return null;
    }

    public static ItemStack findMatchingResult(final InventoryCrafting matrix, final World world) {
        if (ACTIVE.get() == null) return CraftingManager.findMatchingResult(matrix, world);
        final IRecipe recipe = findMatchingRecipe(matrix, world);
        return recipe == null ? ItemStack.EMPTY : recipe.getCraftingResult(matrix);
    }

    private CraftingCandidateIndex index() {
        if (!(ForgeRegistries.RECIPES instanceof RegistryIdLookupAccess recipes)
            || !(ForgeRegistries.ITEMS instanceof RegistryIdLookupAccess items)) return null;
        final long recipeVersion = recipes.nova$getRegistryRevision();
        final long itemVersion = items.nova$getRegistryRevision();
        final long ingredientVersion = INGREDIENT_REVISION.get();
        if (recipeVersion < 0 || itemVersion < 0) return null;
        if (index == null || recipeRevision != recipeVersion || itemRevision != itemVersion
            || ingredientRevision != ingredientVersion) {
            final ObjectArrayList<IRecipe> ordered = new ObjectArrayList<>();
            CraftingManager.REGISTRY.forEach(ordered::add);
            final CraftingCandidateIndex built = new CraftingCandidateIndex(ordered);
            if (recipeVersion != recipes.nova$getRegistryRevision() || itemVersion != items.nova$getRegistryRevision()
                || ingredientVersion != INGREDIENT_REVISION.get()) {
                index = null;
                return null;
            }
            index = built;
            recipeRevision = recipeVersion;
            itemRevision = itemVersion;
            ingredientRevision = ingredientVersion;
        }
        return index;
    }

    @Override
    public void close() {
        index = null;
        if (previous == null) ACTIVE.remove();
        else ACTIVE.set(previous);
    }
}
