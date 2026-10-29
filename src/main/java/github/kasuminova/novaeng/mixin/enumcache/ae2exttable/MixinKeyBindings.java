package github.kasuminova.novaeng.mixin.enumcache.ae2exttable;

import com._0xc4de.ae2exttable.client.KeyBindings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = KeyBindings.class, remap = false)
public abstract class MixinKeyBindings {

    @Shadow
    @Final
    private static KeyBindings[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static KeyBindings[] values() {
        return $VALUES;
    }
}
