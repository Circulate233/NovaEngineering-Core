package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RenderBlockOverlayEvent.OverlayType.class, remap = false)
public abstract class MixinOverlayType {

    @Shadow
    @Final
    private static RenderBlockOverlayEvent.OverlayType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RenderBlockOverlayEvent.OverlayType[] values() {
        return $VALUES;
    }
}
