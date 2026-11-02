package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.renderer.BlockModelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockModelRenderer.Orientation.class, remap = false)
public abstract class MixinOrientation {

    @Shadow
    @Final
    private static BlockModelRenderer.Orientation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockModelRenderer.Orientation[] values() {
        return $VALUES;
    }
}
