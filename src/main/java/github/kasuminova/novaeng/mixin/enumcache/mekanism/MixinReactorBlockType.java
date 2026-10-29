package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.generators.common.block.states.BlockStateReactor.ReactorBlockType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ReactorBlockType.class, remap = false)
public abstract class MixinReactorBlockType {

    @Shadow
    @Final
    private static ReactorBlockType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ReactorBlockType[] values() {
        return $VALUES;
    }
}
