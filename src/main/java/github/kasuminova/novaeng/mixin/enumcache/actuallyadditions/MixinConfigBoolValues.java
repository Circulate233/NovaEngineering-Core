package github.kasuminova.novaeng.mixin.enumcache.actuallyadditions;

import de.ellpeck.actuallyadditions.mod.config.values.ConfigBoolValues;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConfigBoolValues.class, remap = false)
public abstract class MixinConfigBoolValues {

    @Shadow
    @Final
    private static ConfigBoolValues[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ConfigBoolValues[] values() {
        return $VALUES;
    }
}
