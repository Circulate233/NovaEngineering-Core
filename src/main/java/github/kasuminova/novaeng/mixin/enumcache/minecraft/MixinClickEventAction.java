package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.text.event.ClickEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ClickEvent.Action.class, remap = false)
public abstract class MixinClickEventAction {

    @Shadow
    @Final
    private static ClickEvent.Action[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ClickEvent.Action[] values() {
        return $VALUES;
    }
}
