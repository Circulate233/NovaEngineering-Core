package github.kasuminova.novaeng.common.advancement;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.util.Arrays;
import java.util.function.BiPredicate;

/**
 * Reusable item-to-slot chains. Each original stack remains distinct, in inventory order.
 */
public final class IdentityStackIndex<K, S> {
    private final Reference2IntOpenHashMap<K> heads = new Reference2IntOpenHashMap<>();
    private Object[] stacks = new Object[64];
    private int[] next = new int[64];
    private int[] tails = new int[64];
    private int size;

    public IdentityStackIndex() {
        heads.defaultReturnValue(-1);
    }

    public void add(final K key, final S stack) {
        if (size == stacks.length) {
            final int capacity = size * 2;
            stacks = Arrays.copyOf(stacks, capacity);
            next = Arrays.copyOf(next, capacity);
            tails = Arrays.copyOf(tails, capacity);
        }
        stacks[size] = stack;
        next[size] = -1;
        final int head = heads.getInt(key);
        if (head < 0) {
            heads.put(key, size);
            tails[size] = size;
        } else {
            next[tails[head]] = size;
            tails[head] = size;
        }
        size++;
    }

    public boolean contains(final K key) {
        return heads.containsKey(key);
    }

    /**
     * A null key means a wildcard. Predicates may independently match the same original stack.
     */
    @SuppressWarnings("unchecked")
    public <P> boolean matches(final K key, final P predicate, final BiPredicate<P, S> test) {
        if (key == null) {
            for (int i = 0; i < size; i++) {
                if (test.test(predicate, (S) stacks[i])) {
                    return true;
                }
            }
        } else {
            for (int i = heads.getInt(key); i >= 0; i = next[i]) {
                if (test.test(predicate, (S) stacks[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    /**
     * Clear references at the end of every evaluation, including exceptions and zero matches.
     */
    public void clear() {
        Arrays.fill(stacks, 0, size, null);
        heads.clear();
        size = 0;
    }
}
