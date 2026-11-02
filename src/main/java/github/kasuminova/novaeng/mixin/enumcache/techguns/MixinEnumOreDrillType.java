package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.blocks.machines.EnumOreDrillType;

@Mixin(value = EnumOreDrillType.class, remap = false)
public abstract class MixinEnumOreDrillType {

    @Shadow
    @Final
    private static EnumOreDrillType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumOreDrillType[] values() {
        return $VALUES;
    }
}
