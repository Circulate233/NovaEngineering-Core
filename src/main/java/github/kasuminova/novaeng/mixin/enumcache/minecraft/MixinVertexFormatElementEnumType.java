package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = VertexFormatElement.EnumType.class, remap = false)
public abstract class MixinVertexFormatElementEnumType {

    @Shadow
    @Final
    private static VertexFormatElement.EnumType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static VertexFormatElement.EnumType[] values() {
        return $VALUES;
    }
}
