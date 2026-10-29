package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.renderer.BlockModelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockModelRenderer.EnumNeighborInfo.class, remap = false)
public abstract class MixinEnumNeighborInfo {

    @Shadow
    @Final
    private static BlockModelRenderer.EnumNeighborInfo[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockModelRenderer.EnumNeighborInfo[] values() {
        return $VALUES;
    }
}
