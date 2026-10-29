package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.integration.forestry.filters.SpeciesMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SpeciesMode.class, remap = false)
public abstract class MixinSpeciesMode {

    @Shadow
    @Final
    private static SpeciesMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SpeciesMode[] values() {
        return $VALUES;
    }
}
