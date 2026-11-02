package github.kasuminova.novaeng.mixin.enumcache.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDecoration1;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockTypes_MetalDecoration1.class, remap = false)
public abstract class MixinBlockTypes_MetalDecoration1 {

    @Shadow
    @Final
    private static BlockTypes_MetalDecoration1[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockTypes_MetalDecoration1[] values() {
        return $VALUES;
    }
}
