package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.renderer.OpenGlHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = OpenGlHelper.FboMode.class, remap = false)
public abstract class MixinFboMode {

    @Shadow
    @Final
    private static OpenGlHelper.FboMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static OpenGlHelper.FboMode[] values() {
        return $VALUES;
    }
}
