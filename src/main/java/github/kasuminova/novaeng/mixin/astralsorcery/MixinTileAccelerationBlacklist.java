package github.kasuminova.novaeng.mixin.astralsorcery;

import hellfirepvp.astralsorcery.common.base.TileAccelerationBlacklist;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Answers the acceleration blacklist question per tile class instead of per tile entity.
 *
 * <p>{@code canAccelerate} is asked twice per Horologium pace: once for every candidate position the effect probes
 * while filling its list, and once for every element it actually accelerates. Each ask lower-cases the tile class
 * name - which allocates a string - then walks the prefix list and the class list. None of that depends on the tile
 * entity instance or on anything that changes at runtime: it is a pure function of {@code te.getClass()} and the two
 * blacklist lists, which only ever grow through explicit API calls.</p>
 *
 * <p>The per-class answer is therefore memoised, keeping the original evaluation order on a miss. The memo is dropped
 * whenever either list grows, which is the only way an answer can change, so a class a mod blacklists later is still
 * picked up. That turns one list walk per probe into one list walk per tile class for the lifetime of the game.</p>
 */
@Mixin(value = TileAccelerationBlacklist.class, remap = false)
public abstract class MixinTileAccelerationBlacklist {

    @Shadow
    private static List<String> blacklistedPrefixes;

    @Shadow
    private static List<Class<?>> blacklistedClasses;

    @Shadow
    private static List<Class<?>> erroredTiles;

    @Unique
    private static final Map<Class<?>, Boolean> nova$accelerableByClass = new ConcurrentHashMap<>();

    @Unique
    private static int nova$prefixCount = -1;

    @Unique
    private static int nova$classCount = -1;

    @Unique
    private static int nova$erroredCount = -1;

    /**
     * @author circulation
     * @reason Memoise the per-class answer; the computation is a pure function of the tile class and three lists that
     *         only change through explicit calls, which invalidate the memo.
     */
    @Overwrite(remap = false)
    public static boolean canAccelerate(@Nullable final TileEntity te) {
        if (!(te instanceof ITickable)) {
            return false;
        }

        nova$refreshIfListsChanged();
        return nova$accelerableByClass.computeIfAbsent(te.getClass(), MixinTileAccelerationBlacklist::nova$evaluate);
    }

    /**
     * Runs the original checks in their original order for one tile class.
     *
     * <p>The lower-cased class name is compared against the prefixes, then the class list is tested with
     * {@code isAssignableFrom}, and the errored list last.</p>
     *
     * <p>The original stripped a leading {@code [L} from the name before those tests, but it did so after
     * lower-casing, so the check could never match and the strip never ran. It is left out here because the branch is
     * unreachable in both versions - a tile entity's runtime class is never an array type - and keeping it would only
     * suggest it does something.</p>
     *
     * @param tileClass the tile entity class to classify
     * @return whether a tile of this class may be accelerated
     */
    @Unique
    private static boolean nova$evaluate(final Class<?> tileClass) {
        final String lowerCased = tileClass.getName().toLowerCase(Locale.ROOT);

        for (final String prefix : blacklistedPrefixes) {
            if (lowerCased.startsWith(prefix)) {
                return false;
            }
        }

        for (final Class<?> blacklisted : blacklistedClasses) {
            if (blacklisted.isAssignableFrom(tileClass)) {
                return false;
            }
        }

        return !erroredTiles.contains(tileClass);
    }

    /**
     * Drops the memo when any blacklist list grew.
     *
     * <p>All three lists are append-only, so their sizes are a sufficient change signal: any call that could alter an
     * answer bumps one of them.</p>
     */
    @Unique
    private static void nova$refreshIfListsChanged() {
        final int prefixes = blacklistedPrefixes.size();
        final int classes = blacklistedClasses.size();
        final int errored = erroredTiles.size();
        if (prefixes != nova$prefixCount || classes != nova$classCount || errored != nova$erroredCount) {
            nova$prefixCount = prefixes;
            nova$classCount = classes;
            nova$erroredCount = errored;
            nova$accelerableByClass.clear();
        }
    }
}
