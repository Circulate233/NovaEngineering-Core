package github.kasuminova.novaeng.mixin.enumcache.draconicevolution;

import com.brandon3055.draconicevolution.entity.EntityChaosGuardian.EnumBehaviour;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumBehaviour.class, remap = false)
public abstract class MixinEnumBehaviour {

    @Shadow
    @Final
    private static EnumBehaviour[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumBehaviour[] values() {
        return $VALUES;
    }
}
