package github.kasuminova.novaeng.mixin.enumcache.jei;

import mezz.jei.input.InputHandler.KeyBind;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = KeyBind.class, remap = false)
public abstract class MixinKeyBind {

    @Shadow
    @Final
    private static KeyBind[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static KeyBind[] values() {
        return $VALUES;
    }
}
