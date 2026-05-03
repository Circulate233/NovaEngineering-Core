package github.kasuminova.novaeng.mixin.threng;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.phantamanta44.libnine.capability.impl.L9AspectInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = L9AspectInventory.class, remap = false)
public abstract class MixinL9AspectInventory {

    @Redirect(method = "insertItem", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", ordinal = 0))
    private int n$fixInsertItem(int a, int b, @Local(name = "stack") ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), Math.min(a, b));
    }

}
