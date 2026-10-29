package github.kasuminova.novaeng.mixin.enumcache.advancedrocketry;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zmaster587.advancedRocketry.block.plant.BlockLightwoodPlanks.EnumType;

@Mixin(value = EnumType.class, remap = false)
public abstract class MixinEnumType {

    @Shadow
    @Final
    private static EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumType[] values() {
        return $VALUES;
    }
}
