package github.kasuminova.novaeng.mixin.enumcache.reborncore;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import reborncore.common.powerSystem.PowerSystem.EnergySystem;

@Mixin(value = EnergySystem.class, remap = false)
public abstract class MixinEnergySystem {

    @Shadow
    @Final
    private static EnergySystem[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnergySystem[] values() {
        return $VALUES;
    }
}
