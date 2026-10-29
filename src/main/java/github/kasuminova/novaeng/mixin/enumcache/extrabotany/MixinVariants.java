package github.kasuminova.novaeng.mixin.enumcache.extrabotany;

import com.meteor.extrabotany.common.item.equipment.bauble.ItemCosmetic.Variants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

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
