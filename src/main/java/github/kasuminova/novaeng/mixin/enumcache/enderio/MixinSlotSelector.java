package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.handler.darksteel.gui.SlotSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SlotSelector.class, remap = false)
public abstract class MixinSlotSelector {

    @Shadow
    @Final
    private static SlotSelector[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SlotSelector[] values() {
        return $VALUES;
    }
}
