package github.kasuminova.novaeng.common.util;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;

@Desugar
public record SimpleItem(Item item, int meta, NBTTagCompound nbt) {

    public static SimpleItem getInstance(ItemStack stack) {
        return new SimpleItem(stack.getItem(), stack.getMetadata(), stack.getTagCompound());
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
