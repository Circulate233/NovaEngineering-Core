package github.kasuminova.novaeng.mixin.enumcache.konkrete;

import de.keksuccino.konkrete.json.jsonpath.internal.filter.PatternFlag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PatternFlag.class, remap = false)
public abstract class MixinPatternFlag {

    @Shadow
    @Final
    private static PatternFlag[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PatternFlag[] values() {
        return $VALUES;
    }
}
