package github.kasuminova.novaeng.mixin.enumcache.bloodmagic;

import WayofTime.bloodmagic.incense.EnumTranquilityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumTranquilityType.class, remap = false)
public abstract class MixinEnumTranquilityType {

    @Shadow
    @Final
    private static EnumTranquilityType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumTranquilityType[] values() {
        return $VALUES;
    }
}
