package github.kasuminova.novaeng.mixin.enumcache.jei;

import mezz.jei.network.PacketIdServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PacketIdServer.class, remap = false)
public abstract class MixinPacketIdServer {

    @Shadow
    @Final
    private static PacketIdServer[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PacketIdServer[] values() {
        return $VALUES;
    }
}
