package github.kasuminova.novaeng.mixin.enumcache.jei;

import mezz.jei.config.Config.SearchMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SearchMode.class, remap = false)
public abstract class MixinSearchMode {

    @Shadow
    @Final
    private static SearchMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SearchMode[] values() {
        return $VALUES;
    }
}
