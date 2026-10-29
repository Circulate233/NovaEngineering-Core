package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemModelGenerator.SpanFacing.class, remap = false)
public abstract class MixinSpanFacing {

    @Shadow
    @Final
    private static ItemModelGenerator.SpanFacing[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ItemModelGenerator.SpanFacing[] values() {
        return $VALUES;
    }
}
