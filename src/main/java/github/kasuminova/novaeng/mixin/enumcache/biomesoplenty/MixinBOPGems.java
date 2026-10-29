package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.api.enums.BOPGems;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BOPGems.class, remap = false)
public abstract class MixinBOPGems {

    @Shadow
    @Final
    private static BOPGems[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BOPGems[] values() {
        return $VALUES;
    }
}
