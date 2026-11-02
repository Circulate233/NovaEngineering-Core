package github.kasuminova.novaeng.mixin.enumcache.chisel;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import team.chisel.client.gui.PreviewType;

@Mixin(value = PreviewType.class, remap = false)
public abstract class MixinPreviewType {

    @Shadow
    @Final
    private static PreviewType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PreviewType[] values() {
        return $VALUES;
    }
}
