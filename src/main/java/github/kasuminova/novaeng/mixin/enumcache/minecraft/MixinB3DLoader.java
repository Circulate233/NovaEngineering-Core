package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.model.b3d.B3DLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = B3DLoader.class, remap = false)
public abstract class MixinB3DLoader {

    @Shadow
    @Final
    private static B3DLoader[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static B3DLoader[] values() {
        return $VALUES;
    }
}
