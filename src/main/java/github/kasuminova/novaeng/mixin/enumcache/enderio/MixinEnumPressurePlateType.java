package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.block.painted.EnumPressurePlateType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumPressurePlateType.class, remap = false)
public abstract class MixinEnumPressurePlateType {

    @Shadow
    @Final
    private static EnumPressurePlateType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumPressurePlateType[] values() {
        return $VALUES;
    }
}
