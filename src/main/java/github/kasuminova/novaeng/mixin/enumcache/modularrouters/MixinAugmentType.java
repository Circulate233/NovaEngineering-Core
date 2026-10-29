package github.kasuminova.novaeng.mixin.enumcache.modularrouters;

import me.desht.modularrouters.item.augment.ItemAugment.AugmentType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AugmentType.class, remap = false)
public abstract class MixinAugmentType {

    @Shadow
    @Final
    private static AugmentType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AugmentType[] values() {
        return $VALUES;
    }
}
