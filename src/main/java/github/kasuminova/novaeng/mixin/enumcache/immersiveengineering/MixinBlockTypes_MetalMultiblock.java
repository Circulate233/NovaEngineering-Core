package github.kasuminova.novaeng.mixin.enumcache.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.metal.BlockTypes_MetalMultiblock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockTypes_MetalMultiblock.class, remap = false)
public abstract class MixinBlockTypes_MetalMultiblock {

    @Shadow
    @Final
    private static BlockTypes_MetalMultiblock[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockTypes_MetalMultiblock[] values() {
        return $VALUES;
    }
}
