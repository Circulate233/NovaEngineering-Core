package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.common.entities.item.EntityBOPBoat.Status;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Status.class, remap = false)
public abstract class MixinStatus {

    @Shadow
    @Final
    private static Status[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Status[] values() {
        return $VALUES;
    }
}
