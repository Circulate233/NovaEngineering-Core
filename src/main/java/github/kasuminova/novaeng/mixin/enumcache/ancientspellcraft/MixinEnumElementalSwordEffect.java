package github.kasuminova.novaeng.mixin.enumcache.ancientspellcraft;

import com.windanesz.ancientspellcraft.item.EnumElementalSwordEffect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumElementalSwordEffect.class, remap = false)
public abstract class MixinEnumElementalSwordEffect {

    @Shadow
    @Final
    private static EnumElementalSwordEffect[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumElementalSwordEffect[] values() {
        return $VALUES;
    }
}
