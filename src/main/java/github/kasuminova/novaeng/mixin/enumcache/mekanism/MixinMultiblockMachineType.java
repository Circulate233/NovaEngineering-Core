package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.multiblockmachine.common.block.states.BlockStateMultiblockMachine.MultiblockMachineType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MultiblockMachineType.class, remap = false)
public abstract class MixinMultiblockMachineType {

    @Shadow
    @Final
    private static MultiblockMachineType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MultiblockMachineType[] values() {
        return $VALUES;
    }
}
