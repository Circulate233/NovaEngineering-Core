package github.kasuminova.novaeng.mixin.enumcache.ancientspellcraft;

import com.windanesz.ancientspellcraft.spell.RunewordReach.Effect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Effect.class, remap = false)
public abstract class MixinEffect {

    @Shadow
    @Final
    private static Effect[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Effect[] values() {
        return $VALUES;
    }
}
