package github.kasuminova.novaeng.mixin.enumcache.journeymap;

import journeymap.common.network.packets.crud.WaypointCrudResult.Kind;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Kind.class, remap = false)
public abstract class MixinKind {

    @Shadow
    @Final
    private static Kind[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Kind[] values() {
        return $VALUES;
    }
}
