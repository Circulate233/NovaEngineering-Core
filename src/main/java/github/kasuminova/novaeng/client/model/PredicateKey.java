package github.kasuminova.novaeng.client.model;

import com.google.common.base.Predicate;
import net.minecraft.block.state.IBlockState;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** Immutable compact key for sorted conjunction and disjunction inputs. */
final class PredicateKey implements Iterable<Predicate<IBlockState>> {
    private static final Comparator<Object> HASH_ORDER = Comparator.comparingInt(Object::hashCode);
    private static final ThreadLocal<PredicateKey> LOOKUP = ThreadLocal.withInitial(PredicateKey::new);
    private Object[] predicates;
    private int size;
    private int hash;

    private PredicateKey() {
        predicates = new Object[4];
    }

    private PredicateKey(final Object[] predicates, final int size, final int hash) {
        this.predicates = predicates;
        this.size = size;
        this.hash = hash;
    }

    static PredicateKey from(final Iterable<Predicate<IBlockState>> input) {
        final PredicateKey result = LOOKUP.get();
        result.size = 0;
        for (Predicate<IBlockState> predicate : input) {
            if (predicate != null) {
                if (result.size == result.predicates.length) {
                    result.predicates = Arrays.copyOf(result.predicates, result.size << 1);
                }
                result.predicates[result.size++] = predicate;
            }
        }
        if (result.size > 1) {
            Arrays.sort(result.predicates, 0, result.size, HASH_ORDER);
        }
        int keyHash = 1;
        for (int i = 0; i < result.size; i++) {
            keyHash = 31 * keyHash + result.predicateAt(i).hashCode();
        }
        result.hash = keyHash;
        return result;
    }

    PredicateKey snapshot() {
        return new PredicateKey(Arrays.copyOf(predicates, size), size, hash);
    }

    @Override
    public Iterator<Predicate<IBlockState>> iterator() {
        return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public Predicate<IBlockState> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return predicateAt(index++);
            }
        };
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PredicateKey other) || size != other.size) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (!predicateAt(i).equals(other.predicateAt(i))) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Predicate<IBlockState> predicateAt(final int index) {
        return (Predicate<IBlockState>) predicates[index];
    }
}
