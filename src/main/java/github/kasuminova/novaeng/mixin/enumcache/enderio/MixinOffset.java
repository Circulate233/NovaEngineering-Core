package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.conduit.geom.Offset;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Offset.class, remap = false)
public abstract class MixinOffset {

    @Shadow
    @Final
    private static Offset[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Offset[] values() {
        return $VALUES;
    }
}
