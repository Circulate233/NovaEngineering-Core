package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.settings.KeyModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = KeyModifier.class, remap = false)
public abstract class MixinKeyModifier {

    @Shadow
    @Final
    private static KeyModifier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static KeyModifier[] values() {
        return $VALUES;
    }
}
