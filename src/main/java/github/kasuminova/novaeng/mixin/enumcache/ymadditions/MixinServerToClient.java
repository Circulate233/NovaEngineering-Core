package github.kasuminova.novaeng.mixin.enumcache.ymadditions;

import com.xinyihl.ymadditions.common.network.PacketServerToClient.ServerToClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ServerToClient.class, remap = false)
public abstract class MixinServerToClient {

    @Shadow
    @Final
    private static ServerToClient[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ServerToClient[] values() {
        return $VALUES;
    }
}
