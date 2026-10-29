package github.kasuminova.novaeng.mixin.enumcache.modularrouters;

import me.desht.modularrouters.logic.filter.matchers.InspectionMatcher.InspectionOp;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = InspectionOp.class, remap = false)
public abstract class MixinInspectionOp {

    @Shadow
    @Final
    private static InspectionOp[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static InspectionOp[] values() {
        return $VALUES;
    }
}
