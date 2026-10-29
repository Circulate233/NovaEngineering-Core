package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.common.crafting.altar.recipes.ConstellationRecipe.ConstellationAtlarSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConstellationAtlarSlot.class, remap = false)
public abstract class MixinConstellationAtlarSlot {

    @Shadow
    @Final
    private static ConstellationAtlarSlot[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ConstellationAtlarSlot[] values() {
        return $VALUES;
    }
}
