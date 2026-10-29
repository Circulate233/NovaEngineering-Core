package github.kasuminova.novaeng.mixin.enumcache.jei;

import mezz.jei.network.PacketIdClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PacketIdClient.class, remap = false)
public abstract class MixinPacketIdClient {

    @Shadow
    @Final
    private static PacketIdClient[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PacketIdClient[] values() {
        return $VALUES;
    }
}
