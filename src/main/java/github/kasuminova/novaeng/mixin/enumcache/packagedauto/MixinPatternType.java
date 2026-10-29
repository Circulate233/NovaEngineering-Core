package github.kasuminova.novaeng.mixin.enumcache.packagedauto;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import thelm.packagedauto.api.PatternType;

@Mixin(value = PatternType.class, remap = false)
public abstract class MixinPatternType {

    @Shadow
    @Final
    private static PatternType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PatternType[] values() {
        return $VALUES;
    }
}
