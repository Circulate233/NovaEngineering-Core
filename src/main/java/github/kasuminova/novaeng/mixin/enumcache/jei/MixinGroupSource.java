package github.kasuminova.novaeng.mixin.enumcache.jei;

import mezz.jei.ingredients.group.CollapsedGroupIngredient.GroupSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GroupSource.class, remap = false)
public abstract class MixinGroupSource {

    @Shadow
    @Final
    private static GroupSource[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GroupSource[] values() {
        return $VALUES;
    }
}
