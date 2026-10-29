package github.kasuminova.novaeng.mixin.enumcache.ancientspellcraft;

import com.windanesz.ancientspellcraft.constants.RitualTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RitualTier.class, remap = false)
public abstract class MixinRitualTier {

    @Shadow
    @Final
    private static RitualTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RitualTier[] values() {
        return $VALUES;
    }
}
