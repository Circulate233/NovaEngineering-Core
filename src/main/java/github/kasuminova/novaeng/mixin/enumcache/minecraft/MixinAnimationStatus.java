package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.tileentity.TileEntityShulkerBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TileEntityShulkerBox.AnimationStatus.class, remap = false)
public abstract class MixinAnimationStatus {

    @Shadow
    @Final
    private static TileEntityShulkerBox.AnimationStatus[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TileEntityShulkerBox.AnimationStatus[] values() {
        return $VALUES;
    }
}
