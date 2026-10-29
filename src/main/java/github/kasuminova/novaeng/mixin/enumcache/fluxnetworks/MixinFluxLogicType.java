package github.kasuminova.novaeng.mixin.enumcache.fluxnetworks;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sonar.fluxnetworks.api.network.FluxLogicType;

@Mixin(value = FluxLogicType.class, remap = false)
public abstract class MixinFluxLogicType {

    @Shadow
    @Final
    private static FluxLogicType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FluxLogicType[] values() {
        return $VALUES;
    }
}
