package github.kasuminova.novaeng.mixin.enumcache.bloodmagic;

import WayofTime.bloodmagic.block.enums.BloodRuneType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BloodRuneType.class, remap = false)
public abstract class MixinBloodRuneType {

    @Shadow
    @Final
    private static BloodRuneType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BloodRuneType[] values() {
        return $VALUES;
    }
}
