package github.kasuminova.novaeng.mixin.enumcache.fancymenu;

import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase.Alignment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Alignment.class, remap = false)
public abstract class MixinAlignment {

    @Shadow
    @Final
    private static Alignment[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Alignment[] values() {
        return $VALUES;
    }
}
