package github.kasuminova.novaeng.mixin.enumcache.customloadingscreen;

import alexiil.mc.mod.load.ModLoadingListener.LoaderStage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = LoaderStage.class, remap = false)
public abstract class MixinLoaderStage {

    @Shadow
    @Final
    private static LoaderStage[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static LoaderStage[] values() {
        return $VALUES;
    }
}
