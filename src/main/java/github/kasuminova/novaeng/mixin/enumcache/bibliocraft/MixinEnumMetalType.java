package github.kasuminova.novaeng.mixin.enumcache.bibliocraft;

import jds.bibliocraft.helpers.EnumMetalType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumMetalType.class, remap = false)
public abstract class MixinEnumMetalType {

    @Shadow
    @Final
    private static EnumMetalType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumMetalType[] values() {
        return $VALUES;
    }
}
