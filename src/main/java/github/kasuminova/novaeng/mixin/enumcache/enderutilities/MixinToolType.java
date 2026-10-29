package github.kasuminova.novaeng.mixin.enumcache.enderutilities;

import fi.dy.masa.enderutilities.item.tool.ItemEnderTool.ToolType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ToolType.class, remap = false)
public abstract class MixinToolType {

    @Shadow
    @Final
    private static ToolType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ToolType[] values() {
        return $VALUES;
    }
}
