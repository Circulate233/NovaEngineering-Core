package github.kasuminova.novaeng.client.font;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

/**
 * Small immutable results only: at most 64 inputs, 512 characters each, 8192 input characters in total.
 */
public final class BoundedStringLists {
    private final Object2ObjectLinkedOpenHashMap<String, List<String>> entries = new Object2ObjectLinkedOpenHashMap<>(64);
    private int characters;

    public synchronized List<String> get(final String input) {
        return input != null && input.length() <= 512 ? entries.getAndMoveToLast(input) : null;
    }

    public synchronized void put(final String input, final List<String> result) {
        if (input == null || input.length() > 512 || entries.containsKey(input)) return;
        final List<String> immutable = List.copyOf(result);
        entries.putAndMoveToLast(input, immutable);
        characters += input.length();
        while (entries.size() > 64 || characters > 8192) {
            characters -= entries.firstKey().length();
            entries.removeFirst();
        }
    }

    /**
     * Cache path syntax only; every config lookup still reads the current value.
     */
    public List<String> path(final String input) {
        List<String> result = get(input);
        if (result != null) return result;
        final List<String> parts = new ObjectArrayList<>(4);
        int start = 0;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '.') {
                parts.add(input.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(input.substring(start));
        result = List.copyOf(parts);
        put(input, result);
        return result;
    }
}
