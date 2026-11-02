package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.multiblockmachine.common.block.states.BlockStateMultiblockMachine.MultiblockMachineBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MultiblockMachineBlock.class, remap = false)
public abstract class MixinMultiblockMachineBlock {

    @Shadow
    @Final
    private static MultiblockMachineBlock[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MultiblockMachineBlock[] values() {
        return $VALUES;
    }
}
