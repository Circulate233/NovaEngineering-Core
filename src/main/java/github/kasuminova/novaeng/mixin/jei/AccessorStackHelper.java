package github.kasuminova.novaeng.mixin.jei;

import mezz.jei.startup.StackHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = StackHelper.class, remap = false)
public interface AccessorStackHelper {
    @Accessor("uidCacheEnabled")
    boolean nova$isUidCacheEnabled();
}
