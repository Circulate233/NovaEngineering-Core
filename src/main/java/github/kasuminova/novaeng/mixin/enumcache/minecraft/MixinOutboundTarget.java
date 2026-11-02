package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FMLOutboundHandler.OutboundTarget.class, remap = false)
public abstract class MixinOutboundTarget {

    @Shadow
    @Final
    private static FMLOutboundHandler.OutboundTarget[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FMLOutboundHandler.OutboundTarget[] values() {
        return $VALUES;
    }
}
