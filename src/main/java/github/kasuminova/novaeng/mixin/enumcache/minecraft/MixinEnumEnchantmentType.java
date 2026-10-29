package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.enchantment.EnumEnchantmentType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumEnchantmentType.class, remap = false)
public abstract class MixinEnumEnchantmentType {

    @Shadow
    @Final
    private static EnumEnchantmentType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumEnchantmentType[] values() {
        return $VALUES;
    }
}
