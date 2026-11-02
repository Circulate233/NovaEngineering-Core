package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.model.MultiLayerModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MultiLayerModel.Loader.class, remap = false)
public abstract class MixinMultiLayerModelLoader {

    @Shadow
    @Final
    private static MultiLayerModel.Loader[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MultiLayerModel.Loader[] values() {
        return $VALUES;
    }
}
