package github.kasuminova.novaeng.mixin.enumcache.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.stone.BlockTypes_StoneDevices;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockTypes_StoneDevices.class, remap = false)
public abstract class MixinBlockTypes_StoneDevices {

    @Shadow
    @Final
    private static BlockTypes_StoneDevices[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockTypes_StoneDevices[] values() {
        return $VALUES;
    }
}
