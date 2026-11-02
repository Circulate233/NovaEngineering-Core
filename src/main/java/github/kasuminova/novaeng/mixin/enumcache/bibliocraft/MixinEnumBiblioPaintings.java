package github.kasuminova.novaeng.mixin.enumcache.bibliocraft;

import jds.bibliocraft.helpers.BiblioEnums.EnumBiblioPaintings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumBiblioPaintings.class, remap = false)
public abstract class MixinEnumBiblioPaintings {

    @Shadow
    @Final
    private static EnumBiblioPaintings[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumBiblioPaintings[] values() {
        return $VALUES;
    }
}
