package github.kasuminova.novaeng.mixin.enumcache.celeritasextra;

import com.sumirelabs.celeritasextra.client.gui.CeleritasExtraGameOptions.VerticalSyncOption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = VerticalSyncOption.class, remap = false)
public abstract class MixinVerticalSyncOption {

    @Shadow
    @Final
    private static VerticalSyncOption[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static VerticalSyncOption[] values() {
        return $VALUES;
    }
}
