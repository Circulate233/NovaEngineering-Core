package github.kasuminova.novaeng.mixin.enumcache.bloodmagic;

import WayofTime.bloodmagic.ritual.EnumRuneType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumRuneType.class, remap = false)
public abstract class MixinEnumRuneType {

    @Shadow
    @Final
    private static EnumRuneType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumRuneType[] values() {
        return $VALUES;
    }
}
