package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.model.ItemLayerModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemLayerModel.Loader.class, remap = false)
public abstract class MixinItemLayerModelLoader {

    @Shadow
    @Final
    private static ItemLayerModel.Loader[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ItemLayerModel.Loader[] values() {
        return $VALUES;
    }
}
