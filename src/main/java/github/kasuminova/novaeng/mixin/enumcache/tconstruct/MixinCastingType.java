package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.smeltery.block.BlockCasting.CastingType;

@Mixin(value = CastingType.class, remap = false)
public abstract class MixinCastingType {

    @Shadow
    @Final
    private static CastingType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CastingType[] values() {
        return $VALUES;
    }
}
