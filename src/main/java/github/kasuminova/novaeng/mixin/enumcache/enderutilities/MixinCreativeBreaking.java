package github.kasuminova.novaeng.mixin.enumcache.enderutilities;

import fi.dy.masa.enderutilities.item.tool.ItemEnderTool.CreativeBreaking;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CreativeBreaking.class, remap = false)
public abstract class MixinCreativeBreaking {

    @Shadow
    @Final
    private static CreativeBreaking[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CreativeBreaking[] values() {
        return $VALUES;
    }
}
