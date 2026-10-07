package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

/** Owner-thread reusable frames; nested callbacks cannot read another operation's preparation. */
public final class OperationCache<K, V> {
    private final ObjectArrayList<Reference2ObjectOpenHashMap<K, V>> frames = new ObjectArrayList<>();
    private int depth;

    public void enter() {
        if (depth == frames.size()) {
            frames.add(new Reference2ObjectOpenHashMap<>());
        }
        depth++;
    }

    public void leave() {
        if (depth == 0) {
            throw new IllegalStateException("Unbalanced operation scope");
        }
        frames.get(--depth).clear();
    }

    public boolean active() { return depth != 0; }

    public V get(final K key) {
        return depth == 0 ? null : frames.get(depth - 1).get(key);
    }

    public void put(final K key, final V value) {
        if (depth == 0) {
            throw new IllegalStateException("No active operation");
        }
        final Reference2ObjectOpenHashMap<K, V> frame = frames.get(depth - 1);
        if (frame.size() < 512 || frame.containsKey(key)) {
            frame.put(key, value);
        }
    }
}
