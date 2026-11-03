package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.util.inv.ItemListIgnoreCrafting;
import appeng.util.item.ItemList;
import github.kasuminova.novaeng.common.performance.AvailableStackIteration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Consumer;

@Mixin(value = ItemListIgnoreCrafting.class, remap = false)
public abstract class MixinItemListIgnoreCraftingCopies<T extends IAEStack<T>> implements AvailableStackIteration<T> {
    @Shadow
    @Final
    private IItemList<T> target;

    /**
     * @author NovaEngineering
     * @reason Clear crafting on ItemList's owned copy instead of making an intermediate stack copy.
     */
    @Overwrite
    public void add(T option) {
        if (option != null && option.isCraftable()) {
            if (target.getClass() == ItemList.class) {
                final T previous = target.findPrecise(option);
                final boolean craftable = previous != null && previous.isCraftable();
                target.add(option);
                target.findPrecise(option).setCraftable(craftable);
                return;
            }
            option = option.copy();
            option.setCraftable(false);
        }
        target.add(option);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void nova$forEachAvailable(final Consumer<? super T> consumer) {
        if (target instanceof AvailableStackIteration<?> iteration) {
            ((AvailableStackIteration<T>) iteration).nova$forEachAvailable(consumer);
        } else {
            for (final T stack : target) consumer.accept(stack);
        }
    }
}
