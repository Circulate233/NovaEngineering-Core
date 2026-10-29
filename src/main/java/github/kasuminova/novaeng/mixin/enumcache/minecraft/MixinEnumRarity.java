package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.item.EnumRarity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumRarity.class, remap = false)
public abstract class MixinEnumRarity {

    @Shadow
    @Final
    private static EnumRarity[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumRarity[] values() {
        return $VALUES;
    }
}
