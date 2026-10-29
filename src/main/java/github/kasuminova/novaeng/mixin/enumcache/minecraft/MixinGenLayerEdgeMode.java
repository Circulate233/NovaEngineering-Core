package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.world.gen.layer.GenLayerEdge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GenLayerEdge.Mode.class, remap = false)
public abstract class MixinGenLayerEdgeMode {

    @Shadow
    @Final
    private static GenLayerEdge.Mode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GenLayerEdge.Mode[] values() {
        return $VALUES;
    }
}
