package github.kasuminova.novaeng.mixin.enumcache.fancymenu;

import de.keksuccino.fancymenu.menu.fancy.item.items.ticker.TickerCustomizationItem.TickMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TickMode.class, remap = false)
public abstract class MixinTickMode {

    @Shadow
    @Final
    private static TickMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TickMode[] values() {
        return $VALUES;
    }
}
