package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockRenderLayer.class, remap = false)
public abstract class MixinBlockRenderLayer {

    @Shadow
    @Final
    private static BlockRenderLayer[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockRenderLayer[] values() {
        return $VALUES;
    }
}
