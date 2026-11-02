package github.kasuminova.novaeng.mixin.enumcache.rtg;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import rtg.client.GuiCustomizeWorldScreenRTG.Page;

@Mixin(value = Page.class, remap = false)
public abstract class MixinPage {

    @Shadow
    @Final
    private static Page[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Page[] values() {
        return $VALUES;
    }
}
