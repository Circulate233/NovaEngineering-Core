package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.craftinggrid.CraftingRecipe.CraftMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftMode.class, remap = false)
public abstract class MixinCraftMode {

    @Shadow
    @Final
    private static CraftMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CraftMode[] values() {
        return $VALUES;
    }
}
