package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.multiblock.quantum.QuantumGateEnums.BasicType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BasicType.class, remap = false)
public abstract class MixinBasicType {

    @Shadow
    @Final
    private static BasicType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BasicType[] values() {
        return $VALUES;
    }
}
