package github.kasuminova.novaeng.mixin.enumcache.fluxnetworks;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sonar.fluxnetworks.common.network.PacketTileType;

@Mixin(value = PacketTileType.class, remap = false)
public abstract class MixinPacketTileType {

    @Shadow
    @Final
    private static PacketTileType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PacketTileType[] values() {
        return $VALUES;
    }
}
