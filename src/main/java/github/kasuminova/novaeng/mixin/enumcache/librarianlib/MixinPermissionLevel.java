package github.kasuminova.novaeng.mixin.enumcache.librarianlib;

import com.teamwizardry.librarianlib.features.base.PermissionLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PermissionLevel.class, remap = false)
public abstract class MixinPermissionLevel {

    @Shadow
    @Final
    private static PermissionLevel[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PermissionLevel[] values() {
        return $VALUES;
    }
}
