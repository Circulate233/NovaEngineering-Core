package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.capacitor.DefaultCapacitorData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DefaultCapacitorData.class, remap = false)
public abstract class MixinDefaultCapacitorData {

    @Shadow
    @Final
    private static DefaultCapacitorData[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DefaultCapacitorData[] values() {
        return $VALUES;
    }
}
