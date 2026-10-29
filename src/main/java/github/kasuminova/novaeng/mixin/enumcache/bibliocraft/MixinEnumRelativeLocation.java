package github.kasuminova.novaeng.mixin.enumcache.bibliocraft;

import jds.bibliocraft.helpers.EnumRelativeLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumRelativeLocation.class, remap = false)
public abstract class MixinEnumRelativeLocation {

    @Shadow
    @Final
    private static EnumRelativeLocation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumRelativeLocation[] values() {
        return $VALUES;
    }
}
