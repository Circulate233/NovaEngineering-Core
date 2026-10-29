package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = VertexFormatElement.EnumUsage.class, remap = false)
public abstract class MixinEnumUsage {

    @Shadow
    @Final
    private static VertexFormatElement.EnumUsage[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static VertexFormatElement.EnumUsage[] values() {
        return $VALUES;
    }
}
