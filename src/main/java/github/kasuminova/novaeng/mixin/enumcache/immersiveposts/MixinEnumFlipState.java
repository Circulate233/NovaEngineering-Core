package github.kasuminova.novaeng.mixin.enumcache.immersiveposts;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import twistedgate.immersiveposts.enums.EnumFlipState;

@Mixin(value = EnumFlipState.class, remap = false)
public abstract class MixinEnumFlipState {

    @Shadow
    @Final
    private static EnumFlipState[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumFlipState[] values() {
        return $VALUES;
    }
}
