package github.kasuminova.novaeng.client.model;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;

/**
 * Canonical predicates used by Forge 1.12 multipart model conditions.
 */
public final class PredicateCache {
    private static final ReadMostlyObjectCache<PropertyValueKey, Predicate<IBlockState>> PROPERTY =
        new ReadMostlyObjectCache<>("predicate.property", 192);
    private static final ReadMostlyObjectCache<PredicateKey, Predicate<IBlockState>> AND =
        new ReadMostlyObjectCache<>("predicate.and", 394);
    private static final ReadMostlyObjectCache<PredicateKey, Predicate<IBlockState>> OR =
        new ReadMostlyObjectCache<>("predicate.or", 32);

    private PredicateCache() {
    }

    public static Predicate<IBlockState> property(final IProperty<?> property, final Comparable<?> value) {
        final PropertyValueKey cacheKey = new PropertyValueKey(property, value);
        Predicate<IBlockState> result = PROPERTY.get(cacheKey);
        if (result == null) {
            synchronized (PROPERTY.lock(cacheKey)) {
                result = PROPERTY.get(cacheKey);
                if (result == null) {
                    result = state -> {
                        if (state == null) {
                            return false;
                        }
                        final Comparable<?> actual = state.getValue(property);
                        return value.equals(actual);
                    };
                    PROPERTY.put(cacheKey, result);
                }
            }
        }
        return result;
    }

    public static Predicate<IBlockState> and(final Iterable<Predicate<IBlockState>> input) {
        final PredicateKey cacheKey = PredicateKey.from(input);
        Predicate<IBlockState> result = AND.get(cacheKey);
        if (result == null) {
            synchronized (AND.lock(cacheKey)) {
                result = AND.get(cacheKey);
                if (result == null) {
                    result = Predicates.and(cacheKey);
                    AND.put(cacheKey.snapshot(), result);
                }
            }
        }
        return result;
    }

    public static Predicate<IBlockState> or(final Iterable<Predicate<IBlockState>> input) {
        final PredicateKey cacheKey = PredicateKey.from(input);
        Predicate<IBlockState> result = OR.get(cacheKey);
        if (result == null) {
            synchronized (OR.lock(cacheKey)) {
                result = OR.get(cacheKey);
                if (result == null) {
                    result = Predicates.or(cacheKey);
                    OR.put(cacheKey.snapshot(), result);
                }
            }
        }
        return result;
    }

    public static void clear() {
        PROPERTY.clear();
        AND.clear();
        OR.clear();
    }

    public static String capacityProbe() {
        return PROPERTY.report() + ", " + AND.report() + ", " + OR.report();
    }

    private record PropertyValueKey(IProperty<?> property, Comparable<?> value) {

    }
}
