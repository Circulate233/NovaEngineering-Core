package github.kasuminova.novaeng.mixin.enumcache.fancymenu;

import de.keksuccino.fancymenu.menu.fancy.item.items.text.TextCustomizationItem.SourceMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SourceMode.class, remap = false)
public abstract class MixinSourceMode {

    @Shadow
    @Final
    private static SourceMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SourceMode[] values() {
        return $VALUES;
    }
}
