package github.kasuminova.novaeng.mixin.enumcache.mekeng;

import com.mekeng.github.common.container.handler.GuiFactory.GuiMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiMode.class, remap = false)
public abstract class MixinGuiMode {

    @Shadow
    @Final
    private static GuiMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GuiMode[] values() {
        return $VALUES;
    }
}
