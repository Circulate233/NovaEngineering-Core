package github.kasuminova.novaeng.mixin.enumcache.modularrouters;

import me.desht.modularrouters.network.FilterSettingsMessage.Operation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Operation.class, remap = false)
public abstract class MixinFilterSettingsMessageOperation {

    @Shadow
    @Final
    private static Operation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Operation[] values() {
        return $VALUES;
    }
}
