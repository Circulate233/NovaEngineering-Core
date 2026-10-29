package github.kasuminova.novaeng.mixin.enumcache.actuallyadditions;

import de.ellpeck.actuallyadditions.mod.config.values.ConfigIntListValues;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConfigIntListValues.class, remap = false)
public abstract class MixinConfigIntListValues {

    @Shadow
    @Final
    private static ConfigIntListValues[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ConfigIntListValues[] values() {
        return $VALUES;
    }
}
