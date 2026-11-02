package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraftforge.fml.common.network.handshake.FMLHandshakeClientState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FMLHandshakeClientState.class, remap = false)
public abstract class MixinFMLHandshakeClientState {

    @Shadow
    @Final
    private static FMLHandshakeClientState[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FMLHandshakeClientState[] values() {
        return $VALUES;
    }
}
