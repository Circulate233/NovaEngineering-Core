package github.kasuminova.novaeng.mixin.enumcache.ebwizardry;

import electroblob.wizardry.constants.Element;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Element.class, remap = false)
public abstract class MixinElement {

    @Shadow
    @Final
    private static Element[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Element[] values() {
        return $VALUES;
    }
}
