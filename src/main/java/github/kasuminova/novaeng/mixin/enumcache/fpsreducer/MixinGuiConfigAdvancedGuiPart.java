package github.kasuminova.novaeng.mixin.enumcache.fpsreducer;

import bre.fpsreducer.gui.GuiConfigAdvanced.GuiPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiPart.class, remap = false)
public abstract class MixinGuiConfigAdvancedGuiPart {

    @Shadow
    @Final
    private static GuiPart[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GuiPart[] values() {
        return $VALUES;
    }
}
