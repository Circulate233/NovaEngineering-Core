package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.common.world.BOPWorldSettings.BiomeSize;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BiomeSize.class, remap = false)
public abstract class MixinBiomeSize {

    @Shadow
    @Final
    private static BiomeSize[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BiomeSize[] values() {
        return $VALUES;
    }
}
