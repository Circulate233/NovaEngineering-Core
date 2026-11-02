package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.tile.internal.inventory.ItemOutputSetting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemOutputSetting.class, remap = false)
public abstract class MixinItemOutputSetting {

    @Shadow
    @Final
    private static ItemOutputSetting[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ItemOutputSetting[] values() {
        return $VALUES;
    }
}
