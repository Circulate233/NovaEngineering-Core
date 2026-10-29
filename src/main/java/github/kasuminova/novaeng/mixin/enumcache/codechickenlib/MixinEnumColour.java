package github.kasuminova.novaeng.mixin.enumcache.codechickenlib;

import codechicken.lib.colour.EnumColour;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumColour.class, remap = false)
public abstract class MixinEnumColour {

    @Shadow
    @Final
    private static EnumColour[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumColour[] values() {
        return $VALUES;
    }
}
