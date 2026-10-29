package github.kasuminova.novaeng.mixin.enumcache.enderutilities;

import fi.dy.masa.enderutilities.tileentity.TileEntityInserter.GuiAction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiAction.class, remap = false)
public abstract class MixinGuiAction {

    @Shadow
    @Final
    private static GuiAction[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GuiAction[] values() {
        return $VALUES;
    }
}
