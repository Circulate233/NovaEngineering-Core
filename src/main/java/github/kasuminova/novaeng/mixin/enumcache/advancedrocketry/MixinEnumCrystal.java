package github.kasuminova.novaeng.mixin.enumcache.advancedrocketry;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zmaster587.advancedRocketry.block.BlockCrystal.EnumCrystal;

@Mixin(value = EnumCrystal.class, remap = false)
public abstract class MixinEnumCrystal {

    @Shadow
    @Final
    private static EnumCrystal[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumCrystal[] values() {
        return $VALUES;
    }
}
