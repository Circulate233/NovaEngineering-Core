package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.common.model.animation.JointClips;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = JointClips.IdentityJointClip.class, remap = false)
public abstract class MixinIdentityJointClip {

    @Shadow
    @Final
    private static JointClips.IdentityJointClip[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static JointClips.IdentityJointClip[] values() {
        return $VALUES;
    }
}
