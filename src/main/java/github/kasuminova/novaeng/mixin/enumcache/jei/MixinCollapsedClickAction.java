package github.kasuminova.novaeng.mixin.enumcache.jei;

import mezz.jei.util.CollapsedClickAction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CollapsedClickAction.class, remap = false)
public abstract class MixinCollapsedClickAction {

    @Shadow
    @Final
    private static CollapsedClickAction[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CollapsedClickAction[] values() {
        return $VALUES;
    }
}
