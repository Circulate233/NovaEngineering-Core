package github.kasuminova.novaeng.mixin.enumcache.betterp2p;

import com.projecturanus.betterp2p.client.gui.widget.GuiScale;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiScale.class, remap = false)
public abstract class MixinGuiScale {

    @Shadow
    @Final
    private static GuiScale[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GuiScale[] values() {
        return $VALUES;
    }
}
