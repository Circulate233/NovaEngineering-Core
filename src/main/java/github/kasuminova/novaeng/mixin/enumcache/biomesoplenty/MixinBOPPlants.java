package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.api.enums.BOPPlants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BOPPlants.class, remap = false)
public abstract class MixinBOPPlants {

    @Shadow
    @Final
    private static BOPPlants[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BOPPlants[] values() {
        return $VALUES;
    }
}
