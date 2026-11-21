package github.kasuminova.novaeng.mixin.jei;

import github.kasuminova.novaeng.common.performance.HeiTableMemoryAccess;
import mezz.jei.collect.Table;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = Table.class, remap = false)
public interface MixinHeiTableMemoryAccess extends HeiTableMemoryAccess {
    @Override
    @Accessor("table")
    Map<?, ? extends Map<?, ?>> nova$getRows();
}
