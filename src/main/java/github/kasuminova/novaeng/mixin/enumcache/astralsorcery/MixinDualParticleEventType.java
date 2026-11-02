package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.common.network.packet.server.PktDualParticleEvent.DualParticleEventType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DualParticleEventType.class, remap = false)
public abstract class MixinDualParticleEventType {

    @Shadow
    @Final
    private static DualParticleEventType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DualParticleEventType[] values() {
        return $VALUES;
    }
}
