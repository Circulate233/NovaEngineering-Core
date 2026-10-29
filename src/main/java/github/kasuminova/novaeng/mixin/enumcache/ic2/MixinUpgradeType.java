package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.item.upgrade.ItemUpgradeModule.UpgradeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = UpgradeType.class, remap = false)
public abstract class MixinUpgradeType {

    @Shadow
    @Final
    private static UpgradeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static UpgradeType[] values() {
        return $VALUES;
    }
}
