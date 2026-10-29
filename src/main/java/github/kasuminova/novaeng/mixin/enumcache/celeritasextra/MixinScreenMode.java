package github.kasuminova.novaeng.mixin.enumcache.celeritasextra;

import com.sumirelabs.celeritasextra.client.gui.CeleritasExtraGameOptions.ScreenMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ScreenMode.class, remap = false)
public abstract class MixinScreenMode {

    @Shadow
    @Final
    private static ScreenMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ScreenMode[] values() {
        return $VALUES;
    }
}
