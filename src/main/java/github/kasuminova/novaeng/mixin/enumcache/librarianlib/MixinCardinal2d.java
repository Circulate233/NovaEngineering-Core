package github.kasuminova.novaeng.mixin.enumcache.librarianlib;

import com.teamwizardry.librarianlib.features.math.Cardinal2d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Cardinal2d.class, remap = false)
public abstract class MixinCardinal2d {

    @Shadow
    @Final
    private static Cardinal2d[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Cardinal2d[] values() {
        return $VALUES;
    }
}
