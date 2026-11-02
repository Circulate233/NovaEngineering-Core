package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.generators.common.block.states.BlockStateGenerator.GeneratorBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GeneratorBlock.class, remap = false)
public abstract class MixinGeneratorBlock {

    @Shadow
    @Final
    private static GeneratorBlock[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GeneratorBlock[] values() {
        return $VALUES;
    }
}
