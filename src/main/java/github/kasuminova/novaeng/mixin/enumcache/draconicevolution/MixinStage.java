package github.kasuminova.novaeng.mixin.enumcache.draconicevolution;

import com.brandon3055.draconicevolution.entity.EntityEnderEnergyManipulator.Stage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Stage.class, remap = false)
public abstract class MixinStage {

    @Shadow
    @Final
    private static Stage[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Stage[] values() {
        return $VALUES;
    }
}
