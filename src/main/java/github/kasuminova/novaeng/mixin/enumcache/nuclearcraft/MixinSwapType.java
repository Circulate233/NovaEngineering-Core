package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.multiblock.quantum.QuantumGateEnums.SwapType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SwapType.class, remap = false)
public abstract class MixinSwapType {

    @Shadow
    @Final
    private static SwapType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SwapType[] values() {
        return $VALUES;
    }
}
