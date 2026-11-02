package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RayTraceResult.Type.class, remap = false)
public abstract class MixinRayTraceResultType {

    @Shadow
    @Final
    private static RayTraceResult.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RayTraceResult.Type[] values() {
        return $VALUES;
    }
}
