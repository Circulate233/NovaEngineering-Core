package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.inventory.EntityEquipmentSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntityEquipmentSlot.Type.class, remap = false)
public abstract class MixinEntityEquipmentSlotType {

    @Shadow
    @Final
    private static EntityEquipmentSlot.Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EntityEquipmentSlot.Type[] values() {
        return $VALUES;
    }
}
