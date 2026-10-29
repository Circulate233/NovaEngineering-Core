package github.kasuminova.novaeng.mixin.enumcache.omlib;

import omtteam.omlib.api.permission.EnumAccessLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumAccessLevel.class, remap = false)
public abstract class MixinEnumAccessLevel {

    @Shadow
    @Final
    private static EnumAccessLevel[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumAccessLevel[] values() {
        return $VALUES;
    }
}
