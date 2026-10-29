package github.kasuminova.novaeng.mixin.enumcache.actuallyadditions;

import de.ellpeck.actuallyadditions.mod.config.ConfigCategories;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConfigCategories.class, remap = false)
public abstract class MixinConfigCategories {

    @Shadow
    @Final
    private static ConfigCategories[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ConfigCategories[] values() {
        return $VALUES;
    }
}
