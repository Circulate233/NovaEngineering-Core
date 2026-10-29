package github.kasuminova.novaeng.mixin.enumcache.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalDevice1;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockTypes_MetalDevice1.class, remap = false)
public abstract class MixinBlockTypes_MetalDevice1 {

    @Shadow
    @Final
    private static BlockTypes_MetalDevice1[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockTypes_MetalDevice1[] values() {
        return $VALUES;
    }
}
