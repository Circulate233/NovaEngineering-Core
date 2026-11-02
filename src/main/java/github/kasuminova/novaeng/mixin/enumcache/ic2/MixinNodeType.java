package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.gui.dynamic.GuiParser.NodeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NodeType.class, remap = false)
public abstract class MixinNodeType {

    @Shadow
    @Final
    private static NodeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static NodeType[] values() {
        return $VALUES;
    }
}
