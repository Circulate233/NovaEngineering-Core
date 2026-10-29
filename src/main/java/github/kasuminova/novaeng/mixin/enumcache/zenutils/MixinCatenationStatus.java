package github.kasuminova.novaeng.mixin.enumcache.zenutils;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import youyihj.zenutils.api.util.catenation.CatenationStatus;

@Mixin(value = CatenationStatus.class, remap = false)
public abstract class MixinCatenationStatus {

    @Shadow
    @Final
    private static CatenationStatus[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CatenationStatus[] values() {
        return $VALUES;
    }
}
