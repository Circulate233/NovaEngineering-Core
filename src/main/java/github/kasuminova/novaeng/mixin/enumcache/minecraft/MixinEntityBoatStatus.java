package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.item.EntityBoat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntityBoat.Status.class, remap = false)
public abstract class MixinEntityBoatStatus {

    @Shadow
    @Final
    private static EntityBoat.Status[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntityBoat.Status[] values() {
        return $VALUES;
    }
}
