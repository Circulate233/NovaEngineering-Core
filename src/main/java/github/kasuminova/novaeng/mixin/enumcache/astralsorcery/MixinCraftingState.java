package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.common.crafting.altar.ActiveCraftingTask.CraftingState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftingState.class, remap = false)
public abstract class MixinCraftingState {

    @Shadow
    @Final
    private static CraftingState[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CraftingState[] values() {
        return $VALUES;
    }
}
