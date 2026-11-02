package github.kasuminova.novaeng.mixin.enumcache.armourers_workshop;

import moe.plushie.armourers_workshop.common.addons.ModAddonManager.ItemOverrideType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemOverrideType.class, remap = false)
public abstract class MixinItemOverrideType {

    @Shadow
    @Final
    private static ItemOverrideType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ItemOverrideType[] values() {
        return $VALUES;
    }
}
