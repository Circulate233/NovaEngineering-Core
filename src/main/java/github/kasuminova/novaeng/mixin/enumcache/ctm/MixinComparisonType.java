package github.kasuminova.novaeng.mixin.enumcache.ctm;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import team.chisel.ctm.client.util.BlockstatePredicateParser.ComparisonType;

@Mixin(value = ComparisonType.class, remap = false)
public abstract class MixinComparisonType {

    @Shadow
    @Final
    private static ComparisonType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ComparisonType[] values() {
        return $VALUES;
    }
}
