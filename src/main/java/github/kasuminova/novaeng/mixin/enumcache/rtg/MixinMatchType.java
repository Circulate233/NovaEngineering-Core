package github.kasuminova.novaeng.mixin.enumcache.rtg;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import rtg.api.util.BlockUtil.MatchType;

@Mixin(value = MatchType.class, remap = false)
public abstract class MixinMatchType {

    @Shadow
    @Final
    private static MatchType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MatchType[] values() {
        return $VALUES;
    }
}
