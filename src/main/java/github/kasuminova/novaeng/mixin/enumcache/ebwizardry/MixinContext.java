package github.kasuminova.novaeng.mixin.enumcache.ebwizardry;

import electroblob.wizardry.util.SpellProperties.Context;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Context.class, remap = false)
public abstract class MixinContext {

    @Shadow
    @Final
    private static Context[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Context[] values() {
        return $VALUES;
    }
}
