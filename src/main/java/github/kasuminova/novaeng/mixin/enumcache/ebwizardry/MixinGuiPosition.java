package github.kasuminova.novaeng.mixin.enumcache.ebwizardry;

import electroblob.wizardry.Settings.GuiPosition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiPosition.class, remap = false)
public abstract class MixinGuiPosition {

    @Shadow
    @Final
    private static GuiPosition[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GuiPosition[] values() {
        return $VALUES;
    }
}
