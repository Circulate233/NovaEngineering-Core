package github.kasuminova.novaeng.mixin.enumcache.enderutilities;

import fi.dy.masa.enderutilities.event.RenderEventHandler.HudAlignment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = HudAlignment.class, remap = false)
public abstract class MixinHudAlignment {

    @Shadow
    @Final
    private static HudAlignment[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static HudAlignment[] values() {
        return $VALUES;
    }
}
