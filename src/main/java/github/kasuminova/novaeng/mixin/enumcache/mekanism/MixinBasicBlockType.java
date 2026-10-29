package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.block.states.BlockStateBasic.BasicBlockType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BasicBlockType.class, remap = false)
public abstract class MixinBasicBlockType {

    @Shadow
    @Final
    private static BasicBlockType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BasicBlockType[] values() {
        return $VALUES;
    }
}
