package github.kasuminova.novaeng.mixin.enumcache.immersiveengineering;

import blusunrize.immersiveengineering.common.items.ItemToolUpgrade.ToolUpgrades;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ToolUpgrades.class, remap = false)
public abstract class MixinToolUpgrades {

    @Shadow
    @Final
    private static ToolUpgrades[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ToolUpgrades[] values() {
        return $VALUES;
    }
}
