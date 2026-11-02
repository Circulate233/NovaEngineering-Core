package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.api.enums.BOPClimates;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BOPClimates.class, remap = false)
public abstract class MixinBOPClimates {

    @Shadow
    @Final
    private static BOPClimates[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BOPClimates[] values() {
        return $VALUES;
    }
}
