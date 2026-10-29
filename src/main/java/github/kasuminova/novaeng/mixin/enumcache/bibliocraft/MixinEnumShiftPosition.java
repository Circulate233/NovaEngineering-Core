package github.kasuminova.novaeng.mixin.enumcache.bibliocraft;

import jds.bibliocraft.helpers.EnumShiftPosition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumShiftPosition.class, remap = false)
public abstract class MixinEnumShiftPosition {

    @Shadow
    @Final
    private static EnumShiftPosition[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumShiftPosition[] values() {
        return $VALUES;
    }
}
