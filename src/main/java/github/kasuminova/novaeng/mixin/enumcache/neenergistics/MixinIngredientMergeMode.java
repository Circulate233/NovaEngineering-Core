package github.kasuminova.novaeng.mixin.enumcache.neenergistics;

import com.github.vfyjxf.nee.config.IngredientMergeMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = IngredientMergeMode.class, remap = false)
public abstract class MixinIngredientMergeMode {

    @Shadow
    @Final
    private static IngredientMergeMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static IngredientMergeMode[] values() {
        return $VALUES;
    }
}
