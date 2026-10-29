package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.tools.common.ToolsItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ToolsItem.class, remap = false)
public abstract class MixinToolsItem {

    @Shadow
    @Final
    private static ToolsItem[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ToolsItem[] values() {
        return $VALUES;
    }
}
