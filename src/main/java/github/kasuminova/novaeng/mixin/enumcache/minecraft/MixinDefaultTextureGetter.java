package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.model.ModelLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModelLoader.DefaultTextureGetter.class, remap = false)
public abstract class MixinDefaultTextureGetter {

    @Shadow
    @Final
    private static ModelLoader.DefaultTextureGetter[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModelLoader.DefaultTextureGetter[] values() {
        return $VALUES;
    }
}
