package github.kasuminova.novaeng.mixin.enumcache.bibliocraft;

import jds.bibliocraft.helpers.EnumVertPosition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumVertPosition.class, remap = false)
public abstract class MixinEnumVertPosition {

    @Shadow
    @Final
    private static EnumVertPosition[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumVertPosition[] values() {
        return $VALUES;
    }
}
