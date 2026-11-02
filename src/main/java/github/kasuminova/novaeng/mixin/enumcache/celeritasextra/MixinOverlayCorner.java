package github.kasuminova.novaeng.mixin.enumcache.celeritasextra;

import com.sumirelabs.celeritasextra.client.gui.CeleritasExtraGameOptions.OverlayCorner;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = OverlayCorner.class, remap = false)
public abstract class MixinOverlayCorner {

    @Shadow
    @Final
    private static OverlayCorner[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static OverlayCorner[] values() {
        return $VALUES;
    }
}
