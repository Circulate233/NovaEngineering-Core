package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.model.ModelDynBucket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModelDynBucket.LoaderDynBucket.class, remap = false)
public abstract class MixinLoaderDynBucket {

    @Shadow
    @Final
    private static ModelDynBucket.LoaderDynBucket[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModelDynBucket.LoaderDynBucket[] values() {
        return $VALUES;
    }
}
