package github.kasuminova.novaeng.mixin.enumcache.ymadditions;

import com.xinyihl.ymadditions.common.network.PacketClientToServer.ClientToServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ClientToServer.class, remap = false)
public abstract class MixinClientToServer {

    @Shadow
    @Final
    private static ClientToServer[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ClientToServer[] values() {
        return $VALUES;
    }
}
