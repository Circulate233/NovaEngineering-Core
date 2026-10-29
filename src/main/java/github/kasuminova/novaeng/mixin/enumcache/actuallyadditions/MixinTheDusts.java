package github.kasuminova.novaeng.mixin.enumcache.actuallyadditions;

import de.ellpeck.actuallyadditions.mod.items.metalists.TheDusts;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TheDusts.class, remap = false)
public abstract class MixinTheDusts {

    @Shadow
    @Final
    private static TheDusts[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TheDusts[] values() {
        return $VALUES;
    }
}
