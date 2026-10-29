package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.generators.common.block.states.BlockStateReactor.ReactorBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ReactorBlock.class, remap = false)
public abstract class MixinReactorBlock {

    @Shadow
    @Final
    private static ReactorBlock[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ReactorBlock[] values() {
        return $VALUES;
    }
}
