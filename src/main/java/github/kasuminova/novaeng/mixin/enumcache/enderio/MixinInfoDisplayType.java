package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.powertools.machine.capbank.InfoDisplayType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = InfoDisplayType.class, remap = false)
public abstract class MixinInfoDisplayType {

    @Shadow
    @Final
    private static InfoDisplayType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static InfoDisplayType[] values() {
        return $VALUES;
    }
}
