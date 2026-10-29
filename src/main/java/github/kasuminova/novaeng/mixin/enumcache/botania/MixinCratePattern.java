package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.CratePattern;

@Mixin(value = CratePattern.class, remap = false)
public abstract class MixinCratePattern {

    @Shadow
    @Final
    private static CratePattern[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CratePattern[] values() {
        return $VALUES;
    }
}
