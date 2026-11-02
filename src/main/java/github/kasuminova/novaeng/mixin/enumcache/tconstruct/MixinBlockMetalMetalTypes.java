package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.shared.block.BlockMetal.MetalTypes;

@Mixin(value = MetalTypes.class, remap = false)
public abstract class MixinBlockMetalMetalTypes {

    @Shadow
    @Final
    private static MetalTypes[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MetalTypes[] values() {
        return $VALUES;
    }
}
