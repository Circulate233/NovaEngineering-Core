package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.api.enums.BOPFlowers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BOPFlowers.class, remap = false)
public abstract class MixinBOPFlowers {

    @Shadow
    @Final
    private static BOPFlowers[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BOPFlowers[] values() {
        return $VALUES;
    }
}
