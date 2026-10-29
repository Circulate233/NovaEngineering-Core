package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.block.quantum.BlockQuantumComputerCodeGenerator.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Type.class, remap = false)
public abstract class MixinBlockQuantumComputerCodeGeneratorType {

    @Shadow
    @Final
    private static Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Type[] values() {
        return $VALUES;
    }
}
