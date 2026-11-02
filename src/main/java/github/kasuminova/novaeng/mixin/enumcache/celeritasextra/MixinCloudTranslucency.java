package github.kasuminova.novaeng.mixin.enumcache.celeritasextra;

import com.sumirelabs.celeritasextra.client.gui.CeleritasExtraGameOptions.CloudTranslucency;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CloudTranslucency.class, remap = false)
public abstract class MixinCloudTranslucency {

    @Shadow
    @Final
    private static CloudTranslucency[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CloudTranslucency[] values() {
        return $VALUES;
    }
}
