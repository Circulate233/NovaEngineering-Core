package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.block.states.BlockStateMachine.MachineBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MachineBlock.class, remap = false)
public abstract class MixinMachineBlock {

    @Shadow
    @Final
    private static MachineBlock[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MachineBlock[] values() {
        return $VALUES;
    }
}
