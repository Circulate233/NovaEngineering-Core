package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

import java.util.Set;

/**
 * Reuses Forge's live read-only type view; additions remain immediately visible.
 */
public final class BiomeTypeViews {
    private static final ThreadLocal<Reference2ObjectOpenHashMap<Biome, View>> VIEWS =
        ThreadLocal.withInitial(Reference2ObjectOpenHashMap::new);

    private BiomeTypeViews() {
    }

    public static boolean hasType(final Biome biome, final BiomeDictionary.Type type) {
        final Reference2ObjectOpenHashMap<Biome, View> views = VIEWS.get();
        View view = views.get(biome);
        if (view == null || view.name != biome.getRegistryName() || view.types.isEmpty()) {
            view = new View(biome.getRegistryName(), BiomeDictionary.getTypes(biome));
            if (views.size() >= 512) views.clear();
            views.put(biome, view);
        }
        return view.types.contains(type);
    }

    private record View(ResourceLocation name, Set<BiomeDictionary.Type> types) {
    }
}
