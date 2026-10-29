package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.invpanel.chest.EnumChestSize;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumChestSize.class, remap = false)
public abstract class MixinEnumChestSize {

    @Shadow
    @Final
    private static EnumChestSize[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumChestSize[] values() {
        return $VALUES;
    }
}
