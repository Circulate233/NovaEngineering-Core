package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.server.network.NetHandlerLoginServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NetHandlerLoginServer.LoginState.class, remap = false)
public abstract class MixinLoginState {

    @Shadow
    @Final
    private static NetHandlerLoginServer.LoginState[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static NetHandlerLoginServer.LoginState[] values() {
        return $VALUES;
    }
}
