package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.enchantment.EnchantmentProtection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnchantmentProtection.Type.class, remap = false)
public abstract class MixinEnchantmentProtectionType {

    @Shadow
    @Final
    private static EnchantmentProtection.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnchantmentProtection.Type[] values() {
        return $VALUES;
    }
}
