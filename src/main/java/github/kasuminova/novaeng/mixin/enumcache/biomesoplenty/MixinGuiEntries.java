package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.client.gui.GuiBOPConfigureWorld.GuiEntries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiEntries.class, remap = false)
public abstract class MixinGuiEntries {

    @Shadow
    @Final
    private static GuiEntries[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GuiEntries[] values() {
        return $VALUES;
    }
}
