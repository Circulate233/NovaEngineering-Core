package github.kasuminova.novaeng.mixin.enumcache.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.wooden.BlockTypes_WoodenDevice0;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockTypes_WoodenDevice0.class, remap = false)
public abstract class MixinBlockTypes_WoodenDevice0 {

    @Shadow
    @Final
    private static BlockTypes_WoodenDevice0[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockTypes_WoodenDevice0[] values() {
        return $VALUES;
    }
}
