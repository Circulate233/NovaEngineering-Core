package github.kasuminova.novaeng.mixin.universaltweaks;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import mod.acgaming.universaltweaks.bugfixes.entities.desync.UTEntityDesync;
import net.minecraftforge.fml.common.registry.EntityEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Remembers the blacklist decision per entity type instead of scanning the list again.
 *
 * <p>The desync fix asks whether an entity is blacklisted for every entity on every update and again
 * for every tracked entry, and each answer walks the configured list of entity entries with
 * {@code List.contains}. The list is built once from the config and only ever read afterwards, and
 * the question only depends on the entity's registry entry, so the answer is memoised per entry. The
 * cache is dropped whenever the list itself is replaced or changes length, which covers a config
 * reload.</p>
 */
@Mixin(value = UTEntityDesync.class, remap = false)
public class MixinUTEntityDesync {

    @Unique
    private static final Object2BooleanMap<EntityEntry> NOVAENG$BLACKLIST_CACHE = new Object2BooleanOpenHashMap<>();

    @Unique
    private static List<?> novaeng$cachedList;

    @Unique
    private static int novaeng$cachedListSize = -1;

    @Redirect(method = "isBlacklisted", at = @At(value = "INVOKE",
        target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"), remap = false, require = 1)
    private static boolean novaeng$memoizedContains(final List<EntityEntry> list, final Object entry) {
        if (entry == null) {
            return list.contains(null);
        }
        if (list != novaeng$cachedList || list.size() != novaeng$cachedListSize) {
            NOVAENG$BLACKLIST_CACHE.clear();
            novaeng$cachedList = list;
            novaeng$cachedListSize = list.size();
        }
        final Boolean known = NOVAENG$BLACKLIST_CACHE.get(entry);
        if (known != null) {
            return known;
        }
        final boolean blacklisted = list.contains(entry);
        NOVAENG$BLACKLIST_CACHE.put((EntityEntry) entry, blacklisted);
        return blacklisted;
    }
}
