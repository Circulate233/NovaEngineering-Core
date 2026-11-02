package github.kasuminova.novaeng.mixin.enumcache.reborncore;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import reborncore.client.guibuilder.GuiBuilder.ProgressDirection;

@Mixin(value = ProgressDirection.class, remap = false)
public abstract class MixinProgressDirection {

    @Shadow
    @Final
    private static ProgressDirection[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ProgressDirection[] values() {
        return $VALUES;
    }
}
