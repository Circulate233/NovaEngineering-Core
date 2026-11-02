package github.kasuminova.novaeng.mixin.enumcache.modularui;

import com.cleanroommc.modularui.holoui.ScreenOrientation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ScreenOrientation.class, remap = false)
public abstract class MixinScreenOrientation {

    @Shadow
    @Final
    private static ScreenOrientation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ScreenOrientation[] values() {
        return $VALUES;
    }
}
