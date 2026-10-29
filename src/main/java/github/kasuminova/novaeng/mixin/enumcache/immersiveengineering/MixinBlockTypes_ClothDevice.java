package github.kasuminova.novaeng.mixin.enumcache.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.cloth.BlockTypes_ClothDevice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockTypes_ClothDevice.class, remap = false)
public abstract class MixinBlockTypes_ClothDevice {

    @Shadow
    @Final
    private static BlockTypes_ClothDevice[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockTypes_ClothDevice[] values() {
        return $VALUES;
    }
}
