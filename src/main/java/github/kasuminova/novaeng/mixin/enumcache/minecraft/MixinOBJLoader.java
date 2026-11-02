package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.model.obj.OBJLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = OBJLoader.class, remap = false)
public abstract class MixinOBJLoader {

    @Shadow
    @Final
    private static OBJLoader[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static OBJLoader[] values() {
        return $VALUES;
    }
}
