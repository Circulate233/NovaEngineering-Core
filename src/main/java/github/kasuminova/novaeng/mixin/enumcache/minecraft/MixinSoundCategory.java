package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.SoundCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SoundCategory.class, remap = false)
public abstract class MixinSoundCategory {

    @Shadow
    @Final
    private static SoundCategory[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SoundCategory[] values() {
        return $VALUES;
    }
}
