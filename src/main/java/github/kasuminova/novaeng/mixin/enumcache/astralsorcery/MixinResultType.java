package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.common.crafting.grindstone.GrindstoneRecipe.ResultType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ResultType.class, remap = false)
public abstract class MixinResultType {

    @Shadow
    @Final
    private static ResultType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ResultType[] values() {
        return $VALUES;
    }
}
