package github.kasuminova.novaeng.mixin.enumcache.fluxnetworks;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sonar.fluxnetworks.api.network.AccessLevel;

@Mixin(value = AccessLevel.class, remap = false)
public abstract class MixinAccessLevel {

    @Shadow
    @Final
    private static AccessLevel[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AccessLevel[] values() {
        return $VALUES;
    }
}
