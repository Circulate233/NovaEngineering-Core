package github.kasuminova.novaeng.mixin.enumcache.immersivepetroleum;

import flaxbeard.immersivepetroleum.common.blocks.metal.BlockTypes_IPMetalDevice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockTypes_IPMetalDevice.class, remap = false)
public abstract class MixinBlockTypes_IPMetalDevice {

    @Shadow
    @Final
    private static BlockTypes_IPMetalDevice[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockTypes_IPMetalDevice[] values() {
        return $VALUES;
    }
}
