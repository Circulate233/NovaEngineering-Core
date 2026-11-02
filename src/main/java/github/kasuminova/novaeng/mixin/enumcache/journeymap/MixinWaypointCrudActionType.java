package github.kasuminova.novaeng.mixin.enumcache.journeymap;

import journeymap.common.network.packets.crud.WaypointCrudActionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = WaypointCrudActionType.class, remap = false)
public abstract class MixinWaypointCrudActionType {

    @Shadow
    @Final
    private static WaypointCrudActionType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static WaypointCrudActionType[] values() {
        return $VALUES;
    }
}
