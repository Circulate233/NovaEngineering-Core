package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.mixin.tconstruct.AccessorTinkerMeltingRegistry;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraftforge.fluids.Fluid;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.smeltery.MeltingRecipe;

import java.util.List;
import java.util.Map;

public final class MeltingRecipeIndex {
    private static final ClassValue<Boolean> IDENTITY_EQUALITY = new ClassValue<>() {
        @Override
        protected Boolean computeValue(final Class<?> type) {
            try {
                return type.getMethod("equals", Object.class).getDeclaringClass() == Object.class;
            } catch (NoSuchMethodException impossible) {
                throw new AssertionError(impossible);
            }
        }
    };
    private static volatile Snapshot snapshot;

    private MeltingRecipeIndex() {
    }

    public static List<MeltingRecipe> candidates(final Fluid fluid) {
        final List<MeltingRecipe> source = AccessorTinkerMeltingRegistry.nova$meltingRecipes();
        if (!(source instanceof RevisionedList<MeltingRecipe> tracked) || !IDENTITY_EQUALITY.get(fluid.getClass())) {
            return TinkerRegistry.getAllMeltingRecipies();
        }
        Snapshot current = snapshot;
        if (current == null || current.source != source || current.revision != tracked.revision()) {
            synchronized (MeltingRecipeIndex.class) {
                current = snapshot;
                if (current == null || current.source != source || current.revision != tracked.revision()) {
                    synchronized (tracked) {
                        final Map<String, List<MeltingRecipe>> buckets = new Object2ObjectOpenHashMap<>();
                        for (final MeltingRecipe recipe : tracked) {
                            buckets.computeIfAbsent(recipe.output.getFluid().getName(), key -> new ObjectArrayList<>()).add(recipe);
                        }
                        buckets.replaceAll((key, value) -> new ObjectImmutableList<>(value));
                        current = new Snapshot(source, tracked.revision(), Object2ObjectMaps.unmodifiable(
                            new Object2ObjectOpenHashMap<>(buckets)));
                        snapshot = current;
                    }
                }
            }
        }
        // Fluid delegates may be retargeted to the canonical instance with the same registered name.
        // The caller retains its original getFluid/equals predicate, including after remapping.
        return current.byName.getOrDefault(fluid.getName(), ObjectLists.emptyList());
    }

    private record Snapshot(List<MeltingRecipe> source, long revision, Map<String, List<MeltingRecipe>> byName) {
    }
}
