package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.block.states.BlockStateBasic.BasicBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BasicBlock.class, remap = false)
public abstract class MixinBasicBlock {

    @Shadow
    @Final
    private static BasicBlock[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BasicBlock[] values() {
        return $VALUES;
    }
}
