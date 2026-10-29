package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.mana.spark.SparkUpgradeType;

@Mixin(value = SparkUpgradeType.class, remap = false)
public abstract class MixinSparkUpgradeType {

    @Shadow
    @Final
    private static SparkUpgradeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SparkUpgradeType[] values() {
        return $VALUES;
    }
}
