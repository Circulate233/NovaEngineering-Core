package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Primitive, thread-confined lookups; ore registration invalidates every worker's generation.
 */
public final class OreIdsCache {
    private static final AtomicLong REVISION = new AtomicLong();
    private static final ThreadLocal<OreIdsCache> LOCAL = ThreadLocal.withInitial(OreIdsCache::new);
    private final Long2ObjectOpenHashMap<int[]> values = new Long2ObjectOpenHashMap<>();
    private long revision = -1;

    private OreIdsCache() {
    }

    public static void invalidate() {
        REVISION.incrementAndGet();
    }

    public static int[] get(final ItemStack stack) {
        if (stack.isEmpty()) return IntArrays.EMPTY_ARRAY;
        final Item item = stack.getItem().delegate.get();
        if (stack.getItem().delegate.name() == null) return OreDictionary.getOreIDs(stack);
        final long key = ((long) Item.getIdFromItem(item) << 32) | (stack.getItemDamage() & 0xFFFFFFFFL);
        final OreIdsCache cache = LOCAL.get();
        final long revision = REVISION.get();
        if (cache.revision != revision) {
            cache.values.clear();
            cache.revision = revision;
        }
        int[] ids = cache.values.get(key);
        if (ids == null) {
            ids = OreDictionary.getOreIDs(stack);
            if (revision == REVISION.get()) {
                if (cache.values.size() >= 16384) cache.values.clear();
                cache.values.put(key, ids);
            }
        }
        return ids;
    }
}
