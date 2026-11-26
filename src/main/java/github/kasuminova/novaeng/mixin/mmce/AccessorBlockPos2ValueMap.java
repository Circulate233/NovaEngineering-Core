package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.mmce.common.util.BlockPos2ValueMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = BlockPos2ValueMap.class, remap = false)
public interface AccessorBlockPos2ValueMap<V> {
    @Accessor("internal")
    Long2ObjectMap<V> nova$internal();
}
