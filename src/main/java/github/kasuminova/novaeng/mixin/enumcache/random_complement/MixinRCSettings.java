package github.kasuminova.novaeng.mixin.enumcache.random_complement;

import com.circulation.random_complement.client.RCSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RCSettings.class, remap = false)
public abstract class MixinRCSettings {

    @Shadow
    @Final
    private static RCSettings[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RCSettings[] values() {
        return $VALUES;
    }
}
