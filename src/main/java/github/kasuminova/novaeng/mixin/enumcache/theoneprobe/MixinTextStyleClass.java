package github.kasuminova.novaeng.mixin.enumcache.theoneprobe;

import mcjty.theoneprobe.api.TextStyleClass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TextStyleClass.class, remap = false)
public abstract class MixinTextStyleClass {

    @Shadow
    @Final
    private static TextStyleClass[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TextStyleClass[] values() {
        return $VALUES;
    }
}
