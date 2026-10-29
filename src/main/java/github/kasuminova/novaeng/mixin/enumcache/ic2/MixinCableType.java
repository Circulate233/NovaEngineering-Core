package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.block.wiring.CableType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CableType.class, remap = false)
public abstract class MixinCableType {

    @Shadow
    @Final
    private static CableType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CableType[] values() {
        return $VALUES;
    }
}
