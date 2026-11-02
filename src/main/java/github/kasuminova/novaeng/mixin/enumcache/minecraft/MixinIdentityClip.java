package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.common.model.animation.Clips;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Clips.IdentityClip.class, remap = false)
public abstract class MixinIdentityClip {

    @Shadow
    @Final
    private static Clips.IdentityClip[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Clips.IdentityClip[] values() {
        return $VALUES;
    }
}
