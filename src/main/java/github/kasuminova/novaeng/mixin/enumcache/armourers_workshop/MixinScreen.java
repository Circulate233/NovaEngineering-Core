package github.kasuminova.novaeng.mixin.enumcache.armourers_workshop;

import moe.plushie.armourers_workshop.client.gui.globallibrary.GuiGlobalLibrary.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Screen.class, remap = false)
public abstract class MixinScreen {

    @Shadow
    @Final
    private static Screen[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Screen[] values() {
        return $VALUES;
    }
}
