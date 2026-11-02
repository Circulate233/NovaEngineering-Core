package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.util.EntityCondition;

@Mixin(value = EntityCondition.class, remap = false)
public abstract class MixinEntityCondition {

    @Shadow
    @Final
    private static EntityCondition[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntityCondition[] values() {
        return $VALUES;
    }
}
