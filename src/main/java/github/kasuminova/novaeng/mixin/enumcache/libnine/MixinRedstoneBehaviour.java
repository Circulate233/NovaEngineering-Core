package github.kasuminova.novaeng.mixin.enumcache.libnine;

import io.github.phantamanta44.libnine.util.world.RedstoneBehaviour;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RedstoneBehaviour.class, remap = false)
public abstract class MixinRedstoneBehaviour {

    @Shadow
    @Final
    private static RedstoneBehaviour[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RedstoneBehaviour[] values() {
        return $VALUES;
    }
}
