package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.world.block.BlockSlimeGrass.FoliageType;

@Mixin(value = FoliageType.class, remap = false)
public abstract class MixinFoliageType {

    @Shadow
    @Final
    private static FoliageType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FoliageType[] values() {
        return $VALUES;
    }
}
