package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.sound.SoundRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SoundRegistry.class, remap = false)
public abstract class MixinSoundRegistry {

    @Shadow
    @Final
    private static SoundRegistry[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SoundRegistry[] values() {
        return $VALUES;
    }
}
