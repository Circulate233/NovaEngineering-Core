package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.block.state.BlockFaceShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockFaceShape.class, remap = false)
public abstract class MixinBlockFaceShape {

    @Shadow
    @Final
    private static BlockFaceShape[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockFaceShape[] values() {
        return $VALUES;
    }
}
