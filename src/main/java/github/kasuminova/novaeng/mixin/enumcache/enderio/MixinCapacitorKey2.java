package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.invpanel.capacitor.CapacitorKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CapacitorKey.class, remap = false)
public abstract class MixinCapacitorKey2 {

    @Shadow
    @Final
    private static CapacitorKey[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CapacitorKey[] values() {
        return $VALUES;
    }
}
