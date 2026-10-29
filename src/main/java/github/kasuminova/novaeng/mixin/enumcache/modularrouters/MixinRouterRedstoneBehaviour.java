package github.kasuminova.novaeng.mixin.enumcache.modularrouters;

import me.desht.modularrouters.logic.RouterRedstoneBehaviour;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RouterRedstoneBehaviour.class, remap = false)
public abstract class MixinRouterRedstoneBehaviour {

    @Shadow
    @Final
    private static RouterRedstoneBehaviour[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RouterRedstoneBehaviour[] values() {
        return $VALUES;
    }
}
