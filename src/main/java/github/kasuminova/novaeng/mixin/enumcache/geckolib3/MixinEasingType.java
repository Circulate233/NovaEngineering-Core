package github.kasuminova.novaeng.mixin.enumcache.geckolib3;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import software.bernie.geckolib3.core.easing.EasingType;

@Mixin(value = EasingType.class, remap = false)
public abstract class MixinEasingType {

    @Shadow
    @Final
    private static EasingType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EasingType[] values() {
        return $VALUES;
    }
}
