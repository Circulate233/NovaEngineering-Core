package github.kasuminova.novaeng.mixin.enumcache.ae2ctl;

import github.kasuminova.ae2ctl.client.gui.widget.impl.craftingtree.Background;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Background.class, remap = false)
public abstract class MixinBackground {

    @Shadow
    @Final
    private static Background[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Background[] values() {
        return $VALUES;
    }
}
