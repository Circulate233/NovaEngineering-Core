package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.AlfPortalState;

@Mixin(value = AlfPortalState.class, remap = false)
public abstract class MixinAlfPortalState {

    @Shadow
    @Final
    private static AlfPortalState[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AlfPortalState[] values() {
        return $VALUES;
    }
}
