package github.kasuminova.novaeng.client.util;

import mezz.jei.util.Log;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Collects the item list of every creative tab concurrently.
 * <p>
 * Each tab's {@code displayAllRelevantItems} walks the whole item registry on its own, so the
 * serial version pays that walk once per tab. The walk itself is read-only over the frozen
 * registries, so the tabs can be filled in parallel before the caller folds the results into the
 * (order sensitive and not thread safe) deduplication state.
 */
public final class JeiItemListCollector {

    private static final int MAX_PARALLELISM = 8;

    public static List<CreativeTabs> displayedTabs(final CreativeTabs[] tabs) {
        return Arrays.stream(tabs)
                     .filter(tab -> tab != CreativeTabs.HOTBAR)
                     .collect(Collectors.toList());
    }

    public static List<NonNullList<ItemStack>> collect(final List<CreativeTabs> tabs) {
        final int parallelism = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() - 1, MAX_PARALLELISM));
        if (parallelism <= 1 || tabs.size() <= 1) {
            return tabs.stream().map(JeiItemListCollector::collectTab).collect(Collectors.toList());
        }

        final ForkJoinPool pool = new ForkJoinPool(parallelism);
        try {
            return pool.submit(() -> tabs.parallelStream()
                                         .map(JeiItemListCollector::collectTab)
                                         .collect(Collectors.toList()))
                       .get();
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException failed) {
            Log.get().error("Failed to collect creative tab items in parallel, falling back to the serial collection.", failed);
        } finally {
            pool.shutdown();
        }
        return tabs.stream().map(JeiItemListCollector::collectTab).collect(Collectors.toList());
    }

    private static NonNullList<ItemStack> collectTab(final CreativeTabs tab) {
        final NonNullList<ItemStack> items = NonNullList.create();
        try {
            tab.displayAllRelevantItems(items);
        } catch (final Throwable failure) {
            Log.get().error("Creative tab crashed while getting items. Some items from this tab will be missing from the item list. {}", tab, failure);
            items.clear();
        }
        return items;
    }

    private JeiItemListCollector() {
    }
}
