package github.kasuminova.novaeng.mixin.enumcache.journeymap;

import journeymap.common.network.packets.WaypointQueryPacket.QueryType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = QueryType.class, remap = false)
public abstract class MixinQueryType {

    @Shadow
    @Final
    private static QueryType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static QueryType[] values() {
        return $VALUES;
    }
}
