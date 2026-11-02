package github.kasuminova.novaeng.mixin.enumcache.ae2exttable;

import com._0xc4de.ae2exttable.client.gui.AE2ExtendedGUIs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AE2ExtendedGUIs.class, remap = false)
public abstract class MixinAE2ExtendedGUIs {

    @Shadow
    @Final
    private static AE2ExtendedGUIs[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AE2ExtendedGUIs[] values() {
        return $VALUES;
    }
}
