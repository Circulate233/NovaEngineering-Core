package github.kasuminova.novaeng.common.util;

import net.minecraft.item.ItemStack;

import java.util.function.ObjDoubleConsumer;

public interface UuGraphIterator {

    void n$forEach(ObjDoubleConsumer<ItemStack> consumer);
}
