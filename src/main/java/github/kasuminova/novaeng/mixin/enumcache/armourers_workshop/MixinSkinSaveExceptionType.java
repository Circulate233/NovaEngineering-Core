package github.kasuminova.novaeng.mixin.enumcache.armourers_workshop;

import moe.plushie.armourers_workshop.common.exception.SkinSaveException.SkinSaveExceptionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SkinSaveExceptionType.class, remap = false)
public abstract class MixinSkinSaveExceptionType {

    @Shadow
    @Final
    private static SkinSaveExceptionType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SkinSaveExceptionType[] values() {
        return $VALUES;
    }
}
