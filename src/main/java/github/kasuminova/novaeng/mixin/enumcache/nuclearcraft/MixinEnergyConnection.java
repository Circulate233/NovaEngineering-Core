package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.tile.internal.energy.EnergyConnection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnergyConnection.class, remap = false)
public abstract class MixinEnergyConnection {

    @Shadow
    @Final
    private static EnergyConnection[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnergyConnection[] values() {
        return $VALUES;
    }
}
