package github.kasuminova.novaeng.mixin.enumcache.cofhcore;

import cofh.core.block.TileCore.TilePacketID;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TilePacketID.class, remap = false)
public abstract class MixinTilePacketID {

    @Shadow
    @Final
    private static TilePacketID[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TilePacketID[] values() {
        return $VALUES;
    }
}
