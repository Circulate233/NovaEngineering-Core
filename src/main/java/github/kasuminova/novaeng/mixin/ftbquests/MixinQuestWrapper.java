package github.kasuminova.novaeng.mixin.ftbquests;

import com.latmod.mods.itemfilters.api.ItemFiltersAPI;
import com.latmod.mods.itemfilters.filters.ORFilter;
import github.kasuminova.novaeng.common.performance.ftb.FtbQuestCandidates;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(targets = "com.feed_the_beast.ftbquests.integration.jei.QuestWrapper", remap = false)
public abstract class MixinQuestWrapper {
    @Redirect(method = "<init>", at = @At(value = "INVOKE",
        target = "Lcom/latmod/mods/itemfilters/filters/ORFilter;getValidItems(Ljava/util/List;)V"), require = 1)
    private void nova$expandTaskItems(final ORFilter filter, final List<ItemStack> list) {
        // This optimization is only for the constructor's fresh, one-shot ORFilter and output.
        // ArrayList is an upstream type check, not our storage: preserve custom sink semantics.
        if (filter.getClass() != ORFilter.class || list.getClass() != ArrayList.class || !list.isEmpty()) {
            filter.getValidItems(list);
            return;
        }
        // Match ORFilter's direct-item fast path, including duplicates and empty stacks.
        for (final ItemStack stack : filter.items) {
            if (ItemFiltersAPI.isFilter(stack)) {
                FtbQuestCandidates.appendFresh(NonNullList::create, candidates -> {
                    // Keep every enumeration and predicate call in the original order. In particular,
                    // dynamic creative subitems and capability filters must not be memoized.
                    for (final Item item : Item.REGISTRY) {
                        item.getSubItems(CreativeTabs.SEARCH, candidates);
                    }
                }, filter::filter, list);
                return;
            }
        }
        list.addAll(filter.items);
    }
}
