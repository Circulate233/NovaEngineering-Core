package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.audio.ISound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ISound.AttenuationType.class, remap = false)
public abstract class MixinAttenuationType {

    @Shadow
    @Final
    private static ISound.AttenuationType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ISound.AttenuationType[] values() {
        return $VALUES;
    }
}
