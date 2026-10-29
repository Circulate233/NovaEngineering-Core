package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.common.item.equipment.bauble.ItemBaubleCosmetic.Variants;

@Mixin(value = Variants.class, remap = false)
public abstract class MixinVariants {

    @Shadow
    @Final
    private static Variants[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Variants[] values() {
        return $VALUES;
    }
}
