package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.item.EnumDyeColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumDyeColor.class, remap = false)
public abstract class MixinEnumDyeColor {

    @Shadow
    @Final
    private static EnumDyeColor[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumDyeColor[] values() {
        return $VALUES;
    }
}
