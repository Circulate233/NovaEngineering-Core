package github.kasuminova.novaeng.mixin.enumcache.actuallyadditions;

import de.ellpeck.actuallyadditions.mod.inventory.GuiHandler.GuiTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiTypes.class, remap = false)
public abstract class MixinGuiTypes {

    @Shadow
    @Final
    private static GuiTypes[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GuiTypes[] values() {
        return $VALUES;
    }
}
