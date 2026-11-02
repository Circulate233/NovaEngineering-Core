package github.kasuminova.novaeng.mixin.enumcache.deepmoblearning;

import mustapelto.deepmoblearning.client.gui.GuiLootFabricator.CraftingError;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftingError.class, remap = false)
public abstract class MixinCraftingError {

    @Shadow
    @Final
    private static CraftingError[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CraftingError[] values() {
        return $VALUES;
    }
}
