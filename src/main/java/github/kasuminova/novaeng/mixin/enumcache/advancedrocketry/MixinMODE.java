package github.kasuminova.novaeng.mixin.enumcache.advancedrocketry;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zmaster587.advancedRocketry.tile.multiblock.orbitallaserdrill.TileOrbitalLaserDrill.MODE;

@Mixin(value = MODE.class, remap = false)
public abstract class MixinMODE {

    @Shadow
    @Final
    private static MODE[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MODE[] values() {
        return $VALUES;
    }
}
