package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RenderGameOverlayEvent.ElementType.class, remap = false)
public abstract class MixinElementType {

    @Shadow
    @Final
    private static RenderGameOverlayEvent.ElementType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RenderGameOverlayEvent.ElementType[] values() {
        return $VALUES;
    }
}
