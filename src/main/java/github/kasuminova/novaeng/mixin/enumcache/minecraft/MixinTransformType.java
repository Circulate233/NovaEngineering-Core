package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemCameraTransforms.TransformType.class, remap = false)
public abstract class MixinTransformType {

    @Shadow
    @Final
    private static ItemCameraTransforms.TransformType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ItemCameraTransforms.TransformType[] values() {
        return $VALUES;
    }
}
