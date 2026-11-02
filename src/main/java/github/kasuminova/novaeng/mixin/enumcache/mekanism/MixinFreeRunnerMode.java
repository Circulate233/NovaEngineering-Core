package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.item.ItemFreeRunners.FreeRunnerMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FreeRunnerMode.class, remap = false)
public abstract class MixinFreeRunnerMode {

    @Shadow
    @Final
    private static FreeRunnerMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FreeRunnerMode[] values() {
        return $VALUES;
    }
}
