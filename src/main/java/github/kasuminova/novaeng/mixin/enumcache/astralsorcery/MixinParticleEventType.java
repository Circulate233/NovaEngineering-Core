package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.common.network.packet.server.PktParticleEvent.ParticleEventType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ParticleEventType.class, remap = false)
public abstract class MixinParticleEventType {

    @Shadow
    @Final
    private static ParticleEventType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ParticleEventType[] values() {
        return $VALUES;
    }
}
