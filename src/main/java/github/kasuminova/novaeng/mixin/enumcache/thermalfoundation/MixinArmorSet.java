package github.kasuminova.novaeng.mixin.enumcache.thermalfoundation;

import cofh.thermalfoundation.init.TFEquipment.ArmorSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ArmorSet.class, remap = false)
public abstract class MixinArmorSet {

    @Shadow
    @Final
    private static ArmorSet[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ArmorSet[] values() {
        return $VALUES;
    }
}
