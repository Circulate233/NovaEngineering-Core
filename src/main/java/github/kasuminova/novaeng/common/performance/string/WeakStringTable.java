package github.kasuminova.novaeng.common.performance.string;

import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * Fixed-size, four-way string index. Neither keys nor values keep a string alive.
 */
public final class WeakStringTable {
    private static final int WAYS = 4;
    private final WeakReference<String>[] entries;
    private final Object[] locks = new Object[32];
    private final int[] victims = new int[32];
    private final int bucketMask;
    private final int maxLength;

    @SuppressWarnings("unchecked")
    public WeakStringTable(final int capacity, final int maxLength) {
        if (capacity < 128 || Integer.bitCount(capacity) != 1 || maxLength < 1) {
            throw new IllegalArgumentException("Expected power-of-two capacity >= 128 and a positive length limit");
        }
        this.entries = new WeakReference[capacity];
        this.bucketMask = capacity / WAYS - 1;
        this.maxLength = maxLength;
        for (int i = 0; i < locks.length; i++) locks[i] = new Object();
    }

    private static boolean matches(final String existing, final String value, final int start, final int end, final boolean lower) {
        if (existing.length() != end - start) return false;
        for (int i = start; i < end; i++) {
            final char c = value.charAt(i);
            if (existing.charAt(i - start) != (lower ? asciiLower(c) : c)) return false;
        }
        return true;
    }

    private static char asciiLower(final char c) {
        return c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c;
    }

    public String canonicalize(final String value) {
        if (value == null) return null;
        if (value.isEmpty()) return "";
        if (value.length() > maxLength) return value;
        // String already caches this hash; registry strings and already-canonical values
        // need neither another character scan nor a per-character equality loop.
        return lookup(value, 0, value.length(), false, value.hashCode());
    }

    public String slice(final String value, final int start, final int end) {
        final int length = end - start;
        if (start < 0 || end > value.length() || length < 0) throw new StringIndexOutOfBoundsException();
        if (length == 0) return "";
        if (start == 0 && end == value.length()) return canonicalize(value);
        if (length > maxLength) return value.substring(start, end);
        int hash = 0;
        for (int i = start; i < end; i++) hash = 31 * hash + value.charAt(i);
        return lookup(value, start, end, false, hash);
    }

    public String lower(final String value, final Locale locale) {
        if (locale != Locale.ROOT) return canonicalize(value.toLowerCase(locale));
        if (value.length() > maxLength) return value.toLowerCase(locale);
        if (value.isEmpty()) return "";
        int hash = 0;
        boolean changed = false;
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c >= 128) return canonicalize(value.toLowerCase(locale));
            final char normalized = asciiLower(c);
            changed |= normalized != c;
            hash = 31 * hash + normalized;
        }
        return lookup(value, 0, value.length(), changed, hash);
    }

    private String lookup(final String value, final int start, final int end, final boolean lower, final int hash) {
        final int bucket = (hash ^ hash >>> 16) & bucketMask;
        final int stripe = bucket & 31;
        synchronized (locks[stripe]) {
            int empty = -1;
            for (int way = 0; way < WAYS; way++) {
                final int slot = bucket * WAYS + way;
                final WeakReference<String> reference = entries[slot];
                final String existing = reference == null ? null : reference.get();
                if (existing == null) {
                    empty = slot;
                } else if (!lower && start == 0 && end == value.length()
                    ? existing.equals(value) : matches(existing, value, start, end, lower)) {
                    return existing;
                }
            }
            final String result = lower ? value.substring(start, end).toLowerCase(Locale.ROOT) : value.substring(start, end);
            final int slot = empty >= 0 ? empty : bucket * WAYS + (victims[stripe]++ & (WAYS - 1));
            entries[slot] = new WeakReference<>(result);
            return result;
        }
    }

    /**
     * Looks up the complete result before allocating a concatenation or its backing array.
     */
    public String join(final String first, final char separator, final String second) {
        return compose(first, separator, second);
    }

    public String concat(final String first, final String second) {
        return compose(first, -1, second);
    }

    private String compose(final String first, final int separator, final String second) {
        final int secondStart = first.length() + (separator >= 0 ? 1 : 0);
        final int length = secondStart + second.length();
        if (length > maxLength) return separator >= 0 ? first + (char) separator + second : first + second;
        int hash = separator >= 0 ? 31 * first.hashCode() + separator : first.hashCode();
        for (int i = 0; i < second.length(); i++) hash = 31 * hash + second.charAt(i);
        final int bucket = (hash ^ hash >>> 16) & bucketMask;
        final int stripe = bucket & 31;
        synchronized (locks[stripe]) {
            int empty = -1;
            for (int way = 0; way < WAYS; way++) {
                final int slot = bucket * WAYS + way;
                final WeakReference<String> reference = entries[slot];
                final String existing = reference == null ? null : reference.get();
                if (existing == null) {
                    empty = slot;
                } else if (existing.length() == length && (separator < 0 || existing.charAt(first.length()) == separator)
                    && existing.regionMatches(0, first, 0, first.length())
                    && existing.regionMatches(secondStart, second, 0, second.length())) {
                    return existing;
                }
            }
            final String result = separator >= 0 ? first + (char) separator + second : first + second;
            final int slot = empty >= 0 ? empty : bucket * WAYS + (victims[stripe]++ & (WAYS - 1));
            entries[slot] = new WeakReference<>(result);
            return result;
        }
    }
}
