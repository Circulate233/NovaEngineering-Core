package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.block.states.BlockStateMachine.MachineType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MachineType.class, remap = false)
public abstract class MixinMachineType {

    @Shadow
    @Final
    private static MachineType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MachineType[] values() {
        return $VALUES;
    }
}
