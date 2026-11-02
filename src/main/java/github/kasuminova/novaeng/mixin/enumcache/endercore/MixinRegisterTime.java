package github.kasuminova.novaeng.mixin.enumcache.endercore;

import com.enderio.core.common.util.RegisterTime;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RegisterTime.class, remap = false)
public abstract class MixinRegisterTime {

    @Shadow
    @Final
    private static RegisterTime[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RegisterTime[] values() {
        return $VALUES;
    }
}
