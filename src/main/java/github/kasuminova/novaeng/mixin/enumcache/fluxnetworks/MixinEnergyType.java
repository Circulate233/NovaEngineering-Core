package github.kasuminova.novaeng.mixin.enumcache.fluxnetworks;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sonar.fluxnetworks.api.utils.EnergyType;

@Mixin(value = EnergyType.class, remap = false)
public abstract class MixinEnergyType {

    @Shadow
    @Final
    private static EnergyType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnergyType[] values() {
        return $VALUES;
    }
}
