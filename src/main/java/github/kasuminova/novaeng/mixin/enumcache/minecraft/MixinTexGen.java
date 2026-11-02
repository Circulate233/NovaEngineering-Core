package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GlStateManager.TexGen.class, remap = false)
public abstract class MixinTexGen {

    @Shadow
    @Final
    private static GlStateManager.TexGen[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GlStateManager.TexGen[] values() {
        return $VALUES;
    }
}
