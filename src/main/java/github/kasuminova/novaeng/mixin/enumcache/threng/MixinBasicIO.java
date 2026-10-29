package github.kasuminova.novaeng.mixin.enumcache.threng;

import io.github.phantamanta44.threng.util.SlotType.BasicIO;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BasicIO.class, remap = false)
public abstract class MixinBasicIO {

    @Shadow
    @Final
    private static BasicIO[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BasicIO[] values() {
        return $VALUES;
    }
}
