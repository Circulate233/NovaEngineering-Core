package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.common.block.tile.TileAnimatedTorch.TorchMode;

@Mixin(value = TorchMode.class, remap = false)
public abstract class MixinTorchMode {

    @Shadow
    @Final
    private static TorchMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TorchMode[] values() {
        return $VALUES;
    }
}
