package github.kasuminova.novaeng.mixin.enderio;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.enderio.core.common.util.stackable.ItemStackThing", remap = false)
public interface AccessorItemStackThing {
    @Accessor("thing")
    ItemStack nova$getStack();
}
