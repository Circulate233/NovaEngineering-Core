package github.kasuminova.novaeng.mixin.enumcache.ebwizardry;

import electroblob.wizardry.event.DiscoverSpellEvent.Source;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Source.class, remap = false)
public abstract class MixinSource {

    @Shadow
    @Final
    private static Source[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Source[] values() {
        return $VALUES;
    }
}
