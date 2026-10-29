package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.shared.block.BlockSoil.SoilTypes;

@Mixin(value = SoilTypes.class, remap = false)
public abstract class MixinSoilTypes {

    @Shadow
    @Final
    private static SoilTypes[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SoilTypes[] values() {
        return $VALUES;
    }
}
