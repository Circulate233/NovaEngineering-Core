package github.kasuminova.novaeng.client.performance;

/** A small, input-keyed cache for immutable parsed config values, including null results. */
public final class BoundedParsedStrings<V> {
    private static final int MAX_KEY_LENGTH = 4096;
    private final Entry<V>[] slots;

    @SuppressWarnings("unchecked")
    public BoundedParsedStrings(final int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity");
        slots = (Entry<V>[]) new Entry<?>[capacity];
    }

    public synchronized Entry<V> get(final String input) {
        if (input == null || input.length() > MAX_KEY_LENGTH) return null;
        final Entry<V> entry = slots[Math.floorMod(input.hashCode(), slots.length)];
        return entry != null && entry.input().equals(input) ? entry : null;
    }

    public synchronized void put(final String input, final V value) {
        if (input != null && input.length() <= MAX_KEY_LENGTH) {
            slots[Math.floorMod(input.hashCode(), slots.length)] = new Entry<>(input, value);
        }
    }

    public record Entry<V>(String input, V value) { }
}
