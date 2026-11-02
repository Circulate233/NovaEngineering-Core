package github.kasuminova.novaeng.mixin.enumcache.farmingforblockheads;

import net.blay09.mods.farmingforblockheads.item.ItemFertilizer.FertilizerType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FertilizerType.class, remap = false)
public abstract class MixinFertilizerType {

    @Shadow
    @Final
    private static FertilizerType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FertilizerType[] values() {
        return $VALUES;
    }
}
