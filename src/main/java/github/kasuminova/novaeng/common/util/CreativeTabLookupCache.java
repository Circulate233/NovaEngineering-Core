package github.kasuminova.novaeng.common.util;

import github.kasuminova.novaeng.mixin.minecraft.AccessorCreativeTabs;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.creativetab.CreativeTabs;

import java.util.Map;

/**
 * Memoises ContentTweaker's creative tab name lookup.
 *
 * <p>The original walks every registered tab on every call and reads each tab's label through Forge's deprecated
 * reflection helper, which resolves the field anew each time and throws once per tab before finding the spelling
 * that exists. Scripts that query a handful of tab names therefore pay that walk, and those exceptions, over and
 * over.</p>
 *
 * <p>Labels are read once per tab through {@link AccessorCreativeTabs}, and the answer for a name (including a
 * missing tab) is remembered. Tab registration is validated through the array length: tabs may still be added
 * while scripts run, so a changed length drops both tables instead of serving a stale answer.</p>
 *
 * <p>Access is guarded by one monitor. The bracket handler that calls this is reached from script parsing, which
 * the compiler itself treats as single threaded, but this list is also reachable through ContentTweaker's own API,
 * so the cheap guard stays.</p>
 */
public final class CreativeTabLookupCache {

    private static final Object LOCK = new Object();
    private static final Object UNREADABLE = new Object();
    private static final Object MISSING = new Object();
    private static final Map<CreativeTabs, Object> LABELS = new Reference2ObjectOpenHashMap<>();
    private static final Map<String, Object> RESOLVED = new Object2ObjectOpenHashMap<>();
    private static int tabCount = -1;

    private CreativeTabLookupCache() {
    }

    /**
     * Resolves a creative tab by its label, matching the original lookup's first hit over the same array.
     *
     * @param name tab name to resolve, compared case insensitively
     * @return the matching tab, or {@code null} when no tab carries that name
     */
    public static CreativeTabs find(final String name) {
        if (name == null) {
            return null;
        }

        synchronized (LOCK) {
            final CreativeTabs[] tabs = CreativeTabs.CREATIVE_TAB_ARRAY;
            if (tabs.length != tabCount) {
                tabCount = tabs.length;
                LABELS.clear();
                RESOLVED.clear();
            }

            final Object cached = RESOLVED.get(name);
            if (cached != null) {
                return cached == MISSING ? null : (CreativeTabs) cached;
            }

            CreativeTabs found = null;
            for (final CreativeTabs tab : tabs) {
                final String label = labelOf(tab);
                if (label != null && name.equalsIgnoreCase(label)) {
                    found = tab;
                    break;
                }
            }
            RESOLVED.put(name, found == null ? MISSING : found);
            return found;
        }
    }

    /**
     * Returns the cached label of one tab.
     *
     * @param tab tab to read
     * @return the tab's label, or {@code null} when it has none
     */
    private static String labelOf(final CreativeTabs tab) {
        final Object cached = LABELS.get(tab);
        if (cached != null) {
            return cached == UNREADABLE ? null : (String) cached;
        }
        final String label = ((AccessorCreativeTabs) tab).nova$getTabLabel();
        LABELS.put(tab, label == null ? UNREADABLE : label);
        return label;
    }

}
