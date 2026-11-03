package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.ItemList;
import github.kasuminova.novaeng.common.performance.AvailableStackIteration;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Mixin(value = ItemList.class, remap = false)
public abstract class MixinItemListBulkIteration implements AvailableStackIteration<IAEItemStack> {
    @Shadow
    @Final
    private Reference2ObjectMap<Item, ?> records;
    @Shadow
    @Final
    private AtomicInteger version;

    @Override
    public void nova$forEachAvailable(final Consumer<? super IAEItemStack> consumer) {
        final int expected = version.get();
        final Consumer<IAEItemStack> available = stack -> {
            if (!stack.isMeaningful()) return;
            if (version.get() != expected) throw new ConcurrentModificationException();
            consumer.accept(stack);
        };
        records.values().forEach(record -> ((AccessorItemVariantRecords) record).nova$records().values().forEach(available));
    }
}
