package github.kasuminova.novaeng.mixin.enumcache.thermalfoundation;

import cofh.thermalfoundation.init.TFEquipment.ToolSetVanilla;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ToolSetVanilla.class, remap = false)
public abstract class MixinToolSetVanilla {

    @Shadow
    @Final
    private static ToolSetVanilla[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ToolSetVanilla[] values() {
        return $VALUES;
    }
}
