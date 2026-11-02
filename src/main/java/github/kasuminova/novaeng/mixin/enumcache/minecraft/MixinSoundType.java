package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.audio.Sound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Sound.Type.class, remap = false)
public abstract class MixinSoundType {

    @Shadow
    @Final
    private static Sound.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Sound.Type[] values() {
        return $VALUES;
    }
}
