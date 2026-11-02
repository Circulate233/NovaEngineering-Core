package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.entity.item.EntityBoat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntityBoat.Type.class, remap = false)
public abstract class MixinEntityBoatType {

    @Shadow
    @Final
    private static EntityBoat.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntityBoat.Type[] values() {
        return $VALUES;
    }
}
