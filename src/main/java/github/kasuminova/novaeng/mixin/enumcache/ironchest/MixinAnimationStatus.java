package github.kasuminova.novaeng.mixin.enumcache.ironchest;

import cpw.mods.ironchest.common.tileentity.shulker.TileEntityIronShulkerBox.AnimationStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AnimationStatus.class, remap = false)
public abstract class MixinAnimationStatus {

    @Shadow
    @Final
    private static AnimationStatus[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AnimationStatus[] values() {
        return $VALUES;
    }
}
