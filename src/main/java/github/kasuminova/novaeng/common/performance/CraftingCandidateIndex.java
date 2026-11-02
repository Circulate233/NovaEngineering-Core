package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.Arrays;
import java.util.List;

/**
 * Thread-confined index for one startup batch. Actual matches and outputs are always evaluated live.
 */
public final class CraftingCandidateIndex {
    private final IRecipe[] recipes;
    private final int[] counts;
    private final Ingredient[] pivots;
    private final Int2ObjectOpenHashMap<Bucket> buckets = new Int2ObjectOpenHashMap<>();
    private final ObjectArrayList<Frame> frames = new ObjectArrayList<>();
    private int depth;

    public CraftingCandidateIndex(final List<IRecipe> orderedRecipes) {
        recipes = orderedRecipes.toArray(IRecipe[]::new);
        counts = new int[recipes.length];
        Arrays.fill(counts, -1);
        pivots = new Ingredient[recipes.length];
        final Reference2BooleanOpenHashMap<List<ItemStack>> validOres = new Reference2BooleanOpenHashMap<>();
        for (int i = 0; i < recipes.length; i++) {
            final Class<?> type = recipes[i].getClass();
            if (type != ShapedRecipes.class && type != ShapelessRecipes.class
                && type != ShapedOreRecipe.class && type != ShapelessOreRecipe.class) {
                continue;
            }
            boolean known = true;
            int count = 0;
            int smallest = Integer.MAX_VALUE;
            Ingredient pivot = null;
            for (final Ingredient ingredient : recipes[i].getIngredients()) {
                if (ingredient == Ingredient.EMPTY) continue;
                int size;
                if (ingredient.getClass() == Ingredient.class && ingredient instanceof IngredientInputs raw) {
                    size = raw.nova$rawIngredientInputs().length;
                    for (final ItemStack stack : raw.nova$rawIngredientInputs()) {
                        if (stack == null || stack.isEmpty()) {
                            known = false;
                            break;
                        }
                    }
                } else if (ingredient.getClass() == OreIngredient.class && ingredient instanceof OreIngredientInputs access) {
                    final List<ItemStack> ores = access.nova$rawOreInputs();
                    boolean valid = validOres.getBoolean(ores);
                    if (!validOres.containsKey(ores)) {
                        valid = true;
                        for (final ItemStack stack : ores) {
                            if (stack == null || stack.isEmpty()) {
                                valid = false;
                                break;
                            }
                        }
                        validOres.put(ores, valid);
                    }
                    known = valid;
                    size = ores.size();
                } else {
                    known = false;
                    size = 0;
                }
                if (!known) break;
                count++;
                if (size < smallest) {
                    smallest = size;
                    pivot = ingredient;
                }
            }
            if (known) {
                counts[i] = count;
                pivots[i] = pivot;
            }
        }
    }

    private static void add(final Reference2ObjectOpenHashMap<Item, IntArrayList> rows, final ReferenceOpenHashSet<Item> seen,
                            final Item item, final int ordinal) {
        if (seen.add(item)) rows.computeIfAbsent(item, key -> new IntArrayList()).add(ordinal);
    }

    public IRecipe find(final InventoryCrafting matrix, final World world) {
        int count = 0;
        for (int i = 0; i < matrix.getSizeInventory(); i++) {
            if (!matrix.getStackInSlot(i).isEmpty()) count++;
        }
        Bucket bucket = buckets.get(count);
        if (bucket == null) {
            bucket = buildBucket(count);
            buckets.put(count, bucket);
        }
        if (depth == frames.size()) frames.add(new Frame());
        final Frame frame = frames.get(depth++);
        try {
            frame.reset(count + 1);
            frame.add(bucket.fallback);
            for (int i = 0; i < matrix.getSizeInventory(); i++) {
                final ItemStack stack = matrix.getStackInSlot(i);
                if (stack.isEmpty() || !frame.remember(stack.getItem())) continue;
                final int[] matches = bucket.byItem.get(stack.getItem());
                if (matches != null) frame.add(matches);
            }
            int ordinal;
            while ((ordinal = frame.next()) >= 0) {
                final IRecipe recipe = recipes[ordinal];
                if (recipe.matches(matrix, world)) return recipe;
            }
            return null;
        } finally {
            frame.clear();
            depth--;
        }
    }

    private Bucket buildBucket(final int count) {
        final IntArrayList fallback = new IntArrayList();
        final Reference2ObjectOpenHashMap<Item, IntArrayList> byItem = new Reference2ObjectOpenHashMap<>();
        final ReferenceOpenHashSet<Item> seen = new ReferenceOpenHashSet<>();
        for (int ordinal = 0; ordinal < recipes.length; ordinal++) {
            if (counts[ordinal] < 0 || counts[ordinal] == 0 && count == 0) {
                fallback.add(ordinal);
            } else if (counts[ordinal] == count) {
                seen.clear();
                final Ingredient pivot = pivots[ordinal];
                if (pivot.getClass() == Ingredient.class) {
                    // Creative-tab expansion can omit wildcard inputs; use the original matching stacks.
                    for (final ItemStack stack : ((IngredientInputs) pivot).nova$rawIngredientInputs()) {
                        add(byItem, seen, stack.getItem(), ordinal);
                    }
                } else {
                    for (final ItemStack stack : ((OreIngredientInputs) pivot).nova$rawOreInputs()) {
                        add(byItem, seen, stack.getItem(), ordinal);
                    }
                }
            }
        }
        final Reference2ObjectOpenHashMap<Item, int[]> frozen = new Reference2ObjectOpenHashMap<>();
        byItem.forEach((item, ordinals) -> frozen.put(item, ordinals.toIntArray()));
        return new Bucket(fallback.toIntArray(), frozen);
    }

    private record Bucket(int[] fallback, Reference2ObjectOpenHashMap<Item, int[]> byItem) {
    }

    private static final class Frame {
        private int[][] rows = new int[10][];
        private int[] positions = new int[10];
        private Item[] items = new Item[9];
        private int rowCount;
        private int itemCount;

        private void reset(final int size) {
            if (rows.length < size) {
                rows = new int[size][];
                positions = new int[size];
                items = new Item[size];
            }
            rowCount = 0;
            itemCount = 0;
        }

        private boolean remember(final Item item) {
            for (int i = 0; i < itemCount; i++) if (items[i] == item) return false;
            items[itemCount++] = item;
            return true;
        }

        private void add(final int[] row) {
            if (row.length != 0) {
                rows[rowCount] = row;
                positions[rowCount++] = 0;
            }
        }

        private int next() {
            int next = Integer.MAX_VALUE;
            for (int i = 0; i < rowCount; i++) {
                if (positions[i] < rows[i].length) next = Math.min(next, rows[i][positions[i]]);
            }
            if (next == Integer.MAX_VALUE) return -1;
            for (int i = 0; i < rowCount; i++) {
                if (positions[i] < rows[i].length && rows[i][positions[i]] == next) positions[i]++;
            }
            return next;
        }

        private void clear() {
            Arrays.fill(rows, 0, rowCount, null);
            Arrays.fill(items, 0, itemCount, null);
        }
    }
}
