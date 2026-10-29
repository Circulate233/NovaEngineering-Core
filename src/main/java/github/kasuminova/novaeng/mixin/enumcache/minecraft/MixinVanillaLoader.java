package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.model.ModelLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModelLoader.VanillaLoader.class, remap = false)
public abstract class MixinVanillaLoader {

    @Shadow
    @Final
    private static ModelLoader.VanillaLoader[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModelLoader.VanillaLoader[] values() {
        return $VALUES;
    }
}
