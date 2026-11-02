package github.kasuminova.novaeng.mixin.enumcache.futuremc;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import thedarkcolour.futuremc.client.gui.GuiType;

@Mixin(value = GuiType.class, remap = false)
public abstract class MixinGuiType {

    @Shadow
    @Final
    private static GuiType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GuiType[] values() {
        return $VALUES;
    }
}
