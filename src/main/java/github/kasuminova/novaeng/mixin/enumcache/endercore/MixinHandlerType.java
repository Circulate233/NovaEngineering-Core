package github.kasuminova.novaeng.mixin.enumcache.endercore;

import com.enderio.core.common.Handlers.HandlerType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = HandlerType.class, remap = false)
public abstract class MixinHandlerType {

    @Shadow
    @Final
    private static HandlerType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static HandlerType[] values() {
        return $VALUES;
    }
}
