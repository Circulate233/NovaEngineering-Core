package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.item.ItemMug.MugType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MugType.class, remap = false)
public abstract class MixinMugType {

    @Shadow
    @Final
    private static MugType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MugType[] values() {
        return $VALUES;
    }
}
