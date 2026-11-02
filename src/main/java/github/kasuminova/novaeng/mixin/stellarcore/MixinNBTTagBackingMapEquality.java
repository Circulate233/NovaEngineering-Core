package github.kasuminova.novaeng.mixin.stellarcore;

import github.kasuminova.stellarcore.common.util.NBTTagBackingMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.NBTBase;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Iterator;
import java.util.Map;

@Mixin(value = NBTTagBackingMap.class, remap = false)
public abstract class MixinNBTTagBackingMapEquality extends Object2ObjectOpenHashMap<String, NBTBase> {

    /**
     * 目标类原来继承通用映射比较；复用 fastutil 条目，保留右侧遍历及左侧值比较的方向。
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Map<?, ?> map) || map.size() != this.size()) {
            return false;
        }
        final var ownEntries = this.object2ObjectEntrySet();
        final Iterator<?> iterator;
        if (other instanceof Object2ObjectMap<?, ?> objectMap) {
            final var otherEntries = objectMap.object2ObjectEntrySet();
            iterator = otherEntries instanceof Object2ObjectMap.FastEntrySet
                ? ((Object2ObjectMap.FastEntrySet<?, ?>) (Object) otherEntries).fastIterator()
                : otherEntries.iterator();
        } else {
            iterator = map.entrySet().iterator();
        }
        while (iterator.hasNext()) {
            if (!ownEntries.contains(iterator.next())) {
                return false;
            }
        }
        return true;
    }
}
