package github.kasuminova.novaeng.common.util;

import com.github.bsideup.jabel.Desugar;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Desugar
public record SimpleItem(Item item, int meta, NBTTagCompound nbt) {

    private static final NBTTagCompound NullNbt = new NBTTagCompound() {
        @Override
        public boolean equals(Object nbt) {
            return nbt == this;
        }

        @Override
        public int hashCode() {
            return Integer.MIN_VALUE;
        }
    };

    private SimpleItem(ItemStack stack) {
        this(stack.getItem(), stack.getItemDamage(), stack.getTagCompound());
    }

    public static final SimpleItem empty = new SimpleItem(ItemStack.EMPTY);
    private static final Reference2ObjectMap<Item, Int2ObjectOpenHashMapS> chane = Reference2ObjectMaps.synchronize(new Reference2ObjectOpenHashMap<>());
    private static final Function<Item, Int2ObjectOpenHashMapS> intMap = item -> new Int2ObjectOpenHashMapS();

    public static SimpleItem getInstance(final ItemStack stack) {
        if (stack.isEmpty()) return empty;
        var nbt = stack.getTagCompound();
        return chane.computeIfAbsent(stack.getItem(), intMap)
                .computeIfAbsent(stack.getItemDamage())
                .computeIfAbsent(nbt == null ? NullNbt : nbt, n -> new SimpleItem(stack));
    }

    public static SimpleItem getNoNBTInstance(final ItemStack stack) {
        if (stack.isEmpty()) return empty;
        return chane.computeIfAbsent(stack.getItem(), intMap)
                .computeIfAbsent(stack.getItemDamage())
                .computeIfAbsent(NullNbt, n -> new SimpleItem(stack));
    }

    public boolean isEmpty() {
        return this == empty;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SimpleItem that = (SimpleItem) o;
        return meta == that.meta && Objects.equals(item, that.item) && Objects.equals(nbt, that.nbt);
    }

    @Override
    public int hashCode() {
        int result = item.hashCode();
        result = 31 * result + meta;
        result = 31 * result + (nbt != null ? nbt.hashCode() : 0);
        return result;
    }

    private static class Int2ObjectOpenHashMapS extends Int2ObjectOpenHashMap<Map<NBTTagCompound, SimpleItem>> {

        public Map<NBTTagCompound, SimpleItem> computeIfAbsent(int key) {
            Map<NBTTagCompound, SimpleItem> v;

            if ((v = get(key)) == null) {
                synchronized (this) {
                    if ((v = get(key)) == null) {
                        v = new ConcurrentHashMap<>();
                        put(key, v);
                    }
                }
            }

            return v;
        }

    }
}