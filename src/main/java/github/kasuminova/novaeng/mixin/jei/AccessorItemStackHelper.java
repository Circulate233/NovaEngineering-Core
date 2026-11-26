package github.kasuminova.novaeng.mixin.jei;

import mezz.jei.plugins.vanilla.ingredients.item.ItemStackHelper;
import mezz.jei.startup.StackHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ItemStackHelper.class, remap = false)
public interface AccessorItemStackHelper {
    @Accessor("stackHelper")
    StackHelper nova$getStackHelper();
}
