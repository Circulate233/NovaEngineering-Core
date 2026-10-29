package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.projectile.EntityArrow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntityArrow.PickupStatus.class, remap = false)
public abstract class MixinPickupStatus {

    @Shadow
    @Final
    private static EntityArrow.PickupStatus[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntityArrow.PickupStatus[] values() {
        return $VALUES;
    }
}
