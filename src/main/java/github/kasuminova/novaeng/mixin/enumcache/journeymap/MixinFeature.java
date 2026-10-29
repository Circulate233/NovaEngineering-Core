package github.kasuminova.novaeng.mixin.enumcache.journeymap;

import journeymap.client.feature.Feature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Feature.class, remap = false)
public abstract class MixinFeature {

    @Shadow
    @Final
    private static Feature[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Feature[] values() {
        return $VALUES;
    }
}
