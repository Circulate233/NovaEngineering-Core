package github.kasuminova.novaeng.mixin.jeresources;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import jeresources.entry.EnchantmentEntry;
import jeresources.jei.enchantment.EnchantmentWrapper;
import jeresources.registry.EnchantmentRegistry;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;

@Mixin(value = EnchantmentWrapper.class, remap = false)
public abstract class MixinEnchantmentWrapper {
    /**
     * @author circulation
     * @reason Keep dynamic applicability and original set order, without a linked node per displayed enchantment.
     */
    @Nullable
    @Overwrite
    public static EnchantmentWrapper create(final ItemStack itemStack) {
        final ObjectArrayList<EnchantmentEntry> entries = new ObjectArrayList<>(
            EnchantmentRegistry.getInstance().getEnchantments(itemStack));
        return entries.isEmpty() ? null : InvokerEnchantmentWrapper.nova$create(itemStack, entries);
    }
}
