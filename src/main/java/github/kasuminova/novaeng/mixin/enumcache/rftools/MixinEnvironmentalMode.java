package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.blocks.environmental.EnvironmentalControllerTileEntity.EnvironmentalMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnvironmentalMode.class, remap = false)
public abstract class MixinEnvironmentalMode {

    @Shadow
    @Final
    private static EnvironmentalMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnvironmentalMode[] values() {
        return $VALUES;
    }
}
