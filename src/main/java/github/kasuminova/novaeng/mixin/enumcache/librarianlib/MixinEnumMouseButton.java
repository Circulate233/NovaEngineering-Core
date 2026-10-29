package github.kasuminova.novaeng.mixin.enumcache.librarianlib;

import com.teamwizardry.librarianlib.features.facade.EnumMouseButton;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumMouseButton.class, remap = false)
public abstract class MixinEnumMouseButton {

    @Shadow
    @Final
    private static EnumMouseButton[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumMouseButton[] values() {
        return $VALUES;
    }
}
