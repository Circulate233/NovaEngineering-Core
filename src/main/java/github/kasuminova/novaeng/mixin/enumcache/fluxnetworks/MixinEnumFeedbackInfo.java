package github.kasuminova.novaeng.mixin.enumcache.fluxnetworks;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sonar.fluxnetworks.api.gui.EnumFeedbackInfo;

@Mixin(value = EnumFeedbackInfo.class, remap = false)
public abstract class MixinEnumFeedbackInfo {

    @Shadow
    @Final
    private static EnumFeedbackInfo[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumFeedbackInfo[] values() {
        return $VALUES;
    }
}
