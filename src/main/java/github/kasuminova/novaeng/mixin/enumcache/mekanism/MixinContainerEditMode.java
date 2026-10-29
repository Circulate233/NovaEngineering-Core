package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.util.FluidContainerUtils.ContainerEditMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ContainerEditMode.class, remap = false)
public abstract class MixinContainerEditMode {

    @Shadow
    @Final
    private static ContainerEditMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ContainerEditMode[] values() {
        return $VALUES;
    }
}
