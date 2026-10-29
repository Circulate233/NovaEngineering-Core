package github.kasuminova.novaeng.mixin.enumcache.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.BlockTypes_MetalsAll;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockTypes_MetalsAll.class, remap = false)
public abstract class MixinBlockTypes_MetalsAll {

    @Shadow
    @Final
    private static BlockTypes_MetalsAll[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockTypes_MetalsAll[] values() {
        return $VALUES;
    }
}
