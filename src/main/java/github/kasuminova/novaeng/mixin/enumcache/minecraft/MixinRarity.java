package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Enchantment.Rarity.class, remap = false)
public abstract class MixinRarity {

    @Shadow
    @Final
    private static Enchantment.Rarity[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Enchantment.Rarity[] values() {
        return $VALUES;
    }
}
