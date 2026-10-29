package github.kasuminova.novaeng.mixin.enumcache.geckolib3;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import software.bernie.shadowed.eliotlash.mclib.math.Operation;

@Mixin(value = Operation.class, remap = false)
public abstract class MixinOperation {

    @Shadow
    @Final
    private static Operation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Operation[] values() {
        return $VALUES;
    }
}
