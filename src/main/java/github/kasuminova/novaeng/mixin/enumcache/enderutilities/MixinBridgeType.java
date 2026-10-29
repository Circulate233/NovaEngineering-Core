package github.kasuminova.novaeng.mixin.enumcache.enderutilities;

import fi.dy.masa.enderutilities.block.BlockEnergyBridge.BridgeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BridgeType.class, remap = false)
public abstract class MixinBridgeType {

    @Shadow
    @Final
    private static BridgeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BridgeType[] values() {
        return $VALUES;
    }
}
