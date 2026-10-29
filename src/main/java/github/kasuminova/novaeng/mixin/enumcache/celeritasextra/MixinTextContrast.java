package github.kasuminova.novaeng.mixin.enumcache.celeritasextra;

import com.sumirelabs.celeritasextra.client.gui.CeleritasExtraGameOptions.TextContrast;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TextContrast.class, remap = false)
public abstract class MixinTextContrast {

    @Shadow
    @Final
    private static TextContrast[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TextContrast[] values() {
        return $VALUES;
    }
}
