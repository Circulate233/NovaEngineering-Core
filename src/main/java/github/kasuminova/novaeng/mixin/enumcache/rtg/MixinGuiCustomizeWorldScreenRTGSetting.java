package github.kasuminova.novaeng.mixin.enumcache.rtg;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import rtg.client.GuiCustomizeWorldScreenRTG.Setting;

@Mixin(value = Setting.class, remap = false)
public abstract class MixinGuiCustomizeWorldScreenRTGSetting {

    @Shadow
    @Final
    private static Setting[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Setting[] values() {
        return $VALUES;
    }
}
