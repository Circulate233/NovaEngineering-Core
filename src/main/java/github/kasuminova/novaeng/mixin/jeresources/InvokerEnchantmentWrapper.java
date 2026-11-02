package github.kasuminova.novaeng.mixin.jeresources;

import jeresources.entry.EnchantmentEntry;
import jeresources.jei.enchantment.EnchantmentWrapper;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(value = EnchantmentWrapper.class, remap = false)
public interface InvokerEnchantmentWrapper {
    @Invoker("<init>")
    static EnchantmentWrapper nova$create(final ItemStack stack, final List<EnchantmentEntry> enchantments) {
        throw new AssertionError();
    }
}
