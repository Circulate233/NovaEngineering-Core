package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.model.animation.ModelBlockAnimation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModelBlockAnimation.Parameter.Type.class, remap = false)
public abstract class MixinParameterType {

    @Shadow
    @Final
    private static ModelBlockAnimation.Parameter.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModelBlockAnimation.Parameter.Type[] values() {
        return $VALUES;
    }
}
