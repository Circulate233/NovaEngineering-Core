package github.kasuminova.novaeng.mixin.enumcache.more_flux_storage;

import com.circulation.more_flux_storage.api.FluxGuiActionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FluxGuiActionType.class, remap = false)
public abstract class MixinFluxGuiActionType {

    @Shadow
    @Final
    private static FluxGuiActionType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FluxGuiActionType[] values() {
        return $VALUES;
    }
}
