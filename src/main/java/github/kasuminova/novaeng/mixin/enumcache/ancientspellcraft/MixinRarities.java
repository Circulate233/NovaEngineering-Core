package github.kasuminova.novaeng.mixin.enumcache.ancientspellcraft;

import com.windanesz.ancientspellcraft.spell.AbsorbArtefact.Rarities;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Rarities.class, remap = false)
public abstract class MixinRarities {

    @Shadow
    @Final
    private static Rarities[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Rarities[] values() {
        return $VALUES;
    }
}
