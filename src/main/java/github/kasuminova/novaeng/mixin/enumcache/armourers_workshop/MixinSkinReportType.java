package github.kasuminova.novaeng.mixin.enumcache.armourers_workshop;

import moe.plushie.armourers_workshop.common.library.global.task.user.GlobalTaskSkinReport.SkinReport.SkinReportType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SkinReportType.class, remap = false)
public abstract class MixinSkinReportType {

    @Shadow
    @Final
    private static SkinReportType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SkinReportType[] values() {
        return $VALUES;
    }
}
