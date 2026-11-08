package github.kasuminova.novaeng.client.util;

import mezz.jei.util.Log;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Collects the item list of every creative tab concurrently.
 * <p>
 * A single registry walk distributes ordinary items to vanilla tab implementations. Custom item
 * callbacks still run for every ordinary tab; custom tab implementations retain their own traversal.
 * Results are folded in the original tab order by the caller.
 */
public final class JeiItemListCollector {

    private static final int MAX_PARALLELISM = 8;
    private static final ClassValue<Boolean> DEFAULT_TAB = new ClassValue<>() {
        @Override
        protected Boolean computeValue(final Class<?> type) {
            if (type == CreativeTabs.class) return true;
            final Class<?> parent = type.getSuperclass();
            return parent != null && DEFAULT_TAB.get(parent)
                && !overrides(type, new String[]{"displayAllRelevantItems", "func_78018_a"}, NonNullList.class);
        }
    };
    private static final ClassValue<Boolean> DEFAULT_ITEM = new ClassValue<>() {
        @Override
        protected Boolean computeValue(final Class<?> type) {
            if (type == Item.class) return true;
            final Class<?> parent = type.getSuperclass();
            if (parent == null || !DEFAULT_ITEM.get(parent)) return false;
            try {
                for (final var method : type.getDeclaredMethods()) {
                    final Class<?>[] parameters;
                    switch (method.getName()) {
                        case "getSubItems", "func_150895_a" -> parameters = new Class<?>[]{CreativeTabs.class, NonNullList.class};
                        case "isInCreativeTab", "func_194125_a" -> parameters = new Class<?>[]{CreativeTabs.class};
                        case "getCreativeTabs", "getCreativeTab", "func_77640_w" -> parameters = new Class<?>[0];
                        default -> { continue; }
                    }
                    if (Arrays.equals(method.getParameterTypes(), parameters)) return false;
                }
            } catch (LinkageError | SecurityException unavailable) {
                return false;
            }
            return true;
        }
    };

    public static List<CreativeTabs> displayedTabs(final CreativeTabs[] tabs) {
        final ArrayList<CreativeTabs> displayed = new ArrayList<>(tabs.length);
        for (final CreativeTabs tab : tabs) {
            if (tab != CreativeTabs.HOTBAR) displayed.add(tab);
        }
        return displayed;
    }

    public static List<NonNullList<ItemStack>> collect(final List<CreativeTabs> tabs) {
        final Map<CreativeTabs, List<Item>> candidates = indexTabs(tabs);
        final int parallelism = Math.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, MAX_PARALLELISM);
        if (parallelism <= 1 || tabs.size() <= 1) {
            return collectSerial(tabs, candidates);
        }

        final ForkJoinPool pool = new ForkJoinPool(parallelism);
        try {
            return pool.submit(() -> tabs.parallelStream()
                                          .map(tab -> collectTab(tab, candidates.get(tab)))
                                          .collect(Collectors.toList()))
                       .get();
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException failed) {
            Log.get().error("Failed to collect creative tab items in parallel, falling back to the serial collection.", failed);
        } finally {
            pool.shutdown();
        }
        return collectSerial(tabs, candidates);
    }

    private static List<NonNullList<ItemStack>> collectSerial(final List<CreativeTabs> tabs,
                                                            final Map<CreativeTabs, List<Item>> candidates) {
        final ArrayList<NonNullList<ItemStack>> result = new ArrayList<>(tabs.size());
        for (final CreativeTabs tab : tabs) result.add(collectTab(tab, candidates.get(tab)));
        return result;
    }

    private static NonNullList<ItemStack> collectTab(final CreativeTabs tab, final List<Item> candidates) {
        final NonNullList<ItemStack> items = NonNullList.create();
        try {
            if (candidates == null) {
                tab.displayAllRelevantItems(items);
            } else {
                for (final Item item : candidates) {
                    item.getSubItems(tab, items);
                }
            }
        } catch (final Throwable failure) {
            Log.get().error("Creative tab crashed while getting items. Some items from this tab will be missing from the item list. {}", tab, failure);
            items.clear();
        }
        return items;
    }

    private static Map<CreativeTabs, List<Item>> indexTabs(final List<CreativeTabs> tabs) {
        final Map<CreativeTabs, List<Item>> result = new IdentityHashMap<>();
        for (final CreativeTabs tab : tabs) {
            if (tab != null && DEFAULT_TAB.get(tab.getClass())) {
                result.put(tab, new ArrayList<>());
            }
        }
        if (result.isEmpty()) {
            return result;
        }
        for (final Item item : Item.REGISTRY) {
            if (DEFAULT_ITEM.get(item.getClass())) {
                final CreativeTabs owner = item.getCreativeTab();
                final List<Item> ownItems = result.get(owner);
                if (ownItems != null) {
                    ownItems.add(item);
                }
                if (owner != null && owner != CreativeTabs.SEARCH) {
                    final List<Item> searchItems = result.get(CreativeTabs.SEARCH);
                    if (searchItems != null) {
                        searchItems.add(item);
                    }
                }
            } else {
                // getSubItems may ignore getCreativeTabs, so membership alone cannot filter custom callbacks.
                for (final List<Item> items : result.values()) {
                    items.add(item);
                }
            }
        }
        return result;
    }

    private static boolean overrides(final Class<?> type, final String[] names, final Class<?>... parameters) {
        try {
            for (final var method : type.getDeclaredMethods()) {
                for (final String name : names) {
                    if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), parameters)) return true;
                }
            }
        } catch (LinkageError | SecurityException unavailable) {
            return true;
        }
        return false;
    }

    private JeiItemListCollector() {
    }
}
