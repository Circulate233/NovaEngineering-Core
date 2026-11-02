package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.renderer.block.model.ModelRotation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModelRotation.class, remap = false)
public abstract class MixinModelRotation {

    @Shadow
    @Final
    private static ModelRotation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModelRotation[] values() {
        return $VALUES;
    }
}
