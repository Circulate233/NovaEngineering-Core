package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.blocks.machines.EnumMultiBlockMachineType;

@Mixin(value = EnumMultiBlockMachineType.class, remap = false)
public abstract class MixinEnumMultiBlockMachineType {

    @Shadow
    @Final
    private static EnumMultiBlockMachineType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumMultiBlockMachineType[] values() {
        return $VALUES;
    }
}
