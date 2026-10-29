package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.common.block.BlockGemCrystals.GrowthStageType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GrowthStageType.class, remap = false)
public abstract class MixinGrowthStageType {

    @Shadow
    @Final
    private static GrowthStageType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GrowthStageType[] values() {
        return $VALUES;
    }
}
