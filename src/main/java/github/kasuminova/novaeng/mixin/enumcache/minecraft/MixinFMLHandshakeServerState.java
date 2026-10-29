package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.fml.common.network.handshake.FMLHandshakeServerState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FMLHandshakeServerState.class, remap = false)
public abstract class MixinFMLHandshakeServerState {

    @Shadow
    @Final
    private static FMLHandshakeServerState[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FMLHandshakeServerState[] values() {
        return $VALUES;
    }
}
