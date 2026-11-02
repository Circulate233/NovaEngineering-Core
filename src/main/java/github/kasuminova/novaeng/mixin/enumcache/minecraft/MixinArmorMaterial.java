package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.item.ItemArmor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemArmor.ArmorMaterial.class, remap = false)
public abstract class MixinArmorMaterial {

    @Shadow
    @Final
    private static ItemArmor.ArmorMaterial[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ItemArmor.ArmorMaterial[] values() {
        return $VALUES;
    }
}
