package github.kasuminova.novaeng.mixin.jei;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.util.JeiItemListCollector;
import mezz.jei.plugins.vanilla.ingredients.item.ItemStackListFactory;
import mezz.jei.startup.StackHelper;
import mezz.jei.util.ErrorUtil;
import mezz.jei.util.Log;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(value = ItemStackListFactory.class, remap = false)
public abstract class MixinItemStackListFactory {

    @Shadow
    private void addItemStack(final StackHelper stackHelper, final ItemStack stack,
                              final List<ItemStack> itemStacks, final Set<String> seen) {
        throw new AssertionError();
    }

    @Shadow
    private void addBlockAndSubBlocks(final StackHelper stackHelper, final Block block,
                                      final List<ItemStack> itemStacks, final Set<String> seen) {
        throw new AssertionError();
    }

    @Shadow
    private void addItemAndSubItems(final StackHelper stackHelper, final Item item,
                                    final List<ItemStack> itemStacks, final Set<String> seen) {
        throw new AssertionError();
    }

    /**
     * Keeps the original order and the original folding into the deduplication state, but fills the
     * creative tabs concurrently instead of one after another. That pass is the expensive part of
     * the item list build (652ms measured, most of it the per-tab walk over the whole item
     * registry); folding its results is left on this thread because it writes the deduplication set
     * and StackHelper's uid cache, neither of which is thread safe.
     */
    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private void nova$createWithParallelTabs(final StackHelper stackHelper,
                                             final CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!NovaEngCoreConfig.CLIENT.optimizeJeiItemList) {
            return;
        }

        final List<ItemStack> itemStacks = new ArrayList<>();
        final Set<String> seen = new HashSet<>();

        final List<CreativeTabs> tabs = JeiItemListCollector.displayedTabs(CreativeTabs.CREATIVE_TAB_ARRAY);
        final List<NonNullList<ItemStack>> tabItems = JeiItemListCollector.collect(tabs);

        for (int i = 0; i < tabs.size(); i++) {
            final CreativeTabs tab = tabs.get(i);
            for (final ItemStack stack : tabItems.get(i)) {
                if (stack.isEmpty()) {
                    Log.get().error("Found an empty itemStack from creative tab: {}", tab);
                    continue;
                }
                if (stack.getMetadata() == 32767) {
                    Log.get().error("Found an itemStack with wildcard metadata from creative tab: {}. {}",
                                    tab, ErrorUtil.getItemStackInfo(stack));
                    continue;
                }
                this.addItemStack(stackHelper, stack, itemStacks, seen);
            }
        }

        for (final Block block : ForgeRegistries.BLOCKS) {
            this.addBlockAndSubBlocks(stackHelper, block, itemStacks, seen);
        }
        for (final Item item : ForgeRegistries.ITEMS) {
            this.addItemAndSubItems(stackHelper, item, itemStacks, seen);
        }

        cir.setReturnValue(itemStacks);
    }
}
