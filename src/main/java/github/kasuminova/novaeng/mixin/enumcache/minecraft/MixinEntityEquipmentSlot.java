package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.inventory.EntityEquipmentSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntityEquipmentSlot.class, remap = false)
public abstract class MixinEntityEquipmentSlot {

    @Shadow
    @Final
    private static EntityEquipmentSlot[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntityEquipmentSlot[] values() {
        return $VALUES;
    }
}
