package github.kasuminova.novaeng.common.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a biome id from an id table instead of from the registry.
 *
 * <p>Forge keeps the ids of a registry in a {@code HashBiMap<Integer, V>}, so
 * {@code RegistryNamespaced#getObjectById} boxes the id and walks a hash bucket for every lookup,
 * and its wrapper never fills the vanilla array that could answer the same query in one step. A
 * world generator asks for a biome by id for every chunk it decorates.
 *
 * <p>The table is a copy of that mapping, so every method that rewrites it has to drop the table;
 * {@code MixinForgeRegistry} does that from the registry itself. A lookup that misses the table
 * falls back to the registry, which also covers a biome registered after the table was built.
 */
public final class NovaBiomeIdCache {

    private static final int EXPECTED_BIOMES = 512;

    private static volatile Int2ObjectMap<Biome> cache;

    private NovaBiomeIdCache() {
    }

    public static void invalidate() {
        cache = null;
    }

    @Nullable
    public static Biome get(final int id) {
        Int2ObjectMap<Biome> table = cache;
        if (table == null) {
            table = build();
        }
        return table.get(id);
    }

    private static Int2ObjectMap<Biome> build() {
        final Int2ObjectMap<Biome> table = new Int2ObjectOpenHashMap<>(EXPECTED_BIOMES);
        for (final Biome biome : Biome.REGISTRY) {
            table.put(Biome.getIdForBiome(biome), biome);
        }
        cache = table;
        return table;
    }

}
