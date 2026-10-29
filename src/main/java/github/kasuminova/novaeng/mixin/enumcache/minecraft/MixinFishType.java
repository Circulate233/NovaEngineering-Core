package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.item.ItemFishFood;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemFishFood.FishType.class, remap = false)
public abstract class MixinFishType {

    @Shadow
    @Final
    private static ItemFishFood.FishType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ItemFishFood.FishType[] values() {
        return $VALUES;
    }
}
