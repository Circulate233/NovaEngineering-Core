package github.kasuminova.novaeng.mixin.enumcache.brandonscore;

import com.brandon3055.brandonscore.client.gui.modulargui.lib.GuiAlign.TextRotation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TextRotation.class, remap = false)
public abstract class MixinTextRotation {

    @Shadow
    @Final
    private static TextRotation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TextRotation[] values() {
        return $VALUES;
    }
}
