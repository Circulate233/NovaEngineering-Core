package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.gui.dynamic.GuiEnvironment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiEnvironment.class, remap = false)
public abstract class MixinGuiEnvironment {

    @Shadow
    @Final
    private static GuiEnvironment[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GuiEnvironment[] values() {
        return $VALUES;
    }
}
