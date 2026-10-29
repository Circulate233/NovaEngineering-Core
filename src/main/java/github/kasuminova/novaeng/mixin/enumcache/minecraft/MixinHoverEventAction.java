package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.text.event.HoverEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = HoverEvent.Action.class, remap = false)
public abstract class MixinHoverEventAction {

    @Shadow
    @Final
    private static HoverEvent.Action[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static HoverEvent.Action[] values() {
        return $VALUES;
    }
}
