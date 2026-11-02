package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.multiblockmachine.common.block.states.BlockStateMultiblockMachineGenerator.MultiblockMachineGeneratorType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MultiblockMachineGeneratorType.class, remap = false)
public abstract class MixinMultiblockMachineGeneratorType {

    @Shadow
    @Final
    private static MultiblockMachineGeneratorType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MultiblockMachineGeneratorType[] values() {
        return $VALUES;
    }
}
