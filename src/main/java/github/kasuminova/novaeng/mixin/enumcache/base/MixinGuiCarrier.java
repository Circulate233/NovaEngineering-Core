package github.kasuminova.novaeng.mixin.enumcache.base;

import com.teamacronymcoders.base.guisystem.GuiCarrier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GuiCarrier.class, remap = false)
public abstract class MixinGuiCarrier {

    @Shadow
    @Final
    private static GuiCarrier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GuiCarrier[] values() {
        return $VALUES;
    }
}
