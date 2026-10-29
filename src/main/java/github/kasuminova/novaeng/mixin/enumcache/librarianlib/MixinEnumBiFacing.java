package github.kasuminova.novaeng.mixin.enumcache.librarianlib;

import com.teamwizardry.librarianlib.features.utilities.EnumBiFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumBiFacing.class, remap = false)
public abstract class MixinEnumBiFacing {

    @Shadow
    @Final
    private static EnumBiFacing[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumBiFacing[] values() {
        return $VALUES;
    }
}
