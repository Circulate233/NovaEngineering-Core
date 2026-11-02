package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.passive.HorseArmorType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = HorseArmorType.class, remap = false)
public abstract class MixinHorseArmorType {

    @Shadow
    @Final
    private static HorseArmorType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static HorseArmorType[] values() {
        return $VALUES;
    }
}
