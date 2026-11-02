package github.kasuminova.novaeng.mixin.enumcache.armourers_workshop;

import moe.plushie.armourers_workshop.proxies.ClientProxy.TexturePaintType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TexturePaintType.class, remap = false)
public abstract class MixinTexturePaintType {

    @Shadow
    @Final
    private static TexturePaintType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TexturePaintType[] values() {
        return $VALUES;
    }
}
