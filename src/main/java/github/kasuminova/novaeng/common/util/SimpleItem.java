package github.kasuminova.novaeng.common.util;

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
import java.util.function.IntFunction;

public record SimpleItem(Item item, int meta, NBTTagCompound nbt) {

    public static final SimpleItem empty = new SimpleItem(ItemStack.EMPTY);
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
    private static final Reference2ObjectMap<Item, Int2ObjectOpenHashMap<Map<NBTTagCompound, SimpleItem>>> chane = Reference2ObjectMaps.synchronize(new Reference2ObjectOpenHashMap<>());

    private SimpleItem(ItemStack stack) {
        this(stack.getItem(), stack.getItemDamage(), copyTag(stack.getTagCompound()));
    }

    public static SimpleItem getInstance(final ItemStack stack) {
        if (stack.isEmpty()) return empty;
        var nbt = copyTag(stack.getTagCompound());
        return chane.computeIfAbsent(stack.getItem(), (Function<Item, Int2ObjectOpenHashMap<Map<NBTTagCompound, SimpleItem>>>) _ -> new Int2ObjectOpenHashMap<>())
                    .computeIfAbsent(stack.getItemDamage(), (IntFunction<Map<NBTTagCompound, SimpleItem>>) _ -> new ConcurrentHashMap<>())
                    .computeIfAbsent(nbt == null ? NullNbt : nbt, _ -> new SimpleItem(stack.getItem(), stack.getItemDamage(), nbt));
    }

    private static NBTTagCompound copyTag(final NBTTagCompound nbt) {
        return nbt == null ? null : nbt.copy();
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
}
