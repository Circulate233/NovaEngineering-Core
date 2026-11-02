package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.api.enums.BOPWoods;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BOPWoods.class, remap = false)
public abstract class MixinBOPWoods {

    @Shadow
    @Final
    private static BOPWoods[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BOPWoods[] values() {
        return $VALUES;
    }
}
