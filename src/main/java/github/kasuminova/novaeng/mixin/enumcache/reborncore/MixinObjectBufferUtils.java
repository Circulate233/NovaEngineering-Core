package github.kasuminova.novaeng.mixin.enumcache.reborncore;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import reborncore.common.network.ObjectBufferUtils;

@Mixin(value = ObjectBufferUtils.class, remap = false)
public abstract class MixinObjectBufferUtils {

    @Shadow
    @Final
    private static ObjectBufferUtils[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ObjectBufferUtils[] values() {
        return $VALUES;
    }
}
