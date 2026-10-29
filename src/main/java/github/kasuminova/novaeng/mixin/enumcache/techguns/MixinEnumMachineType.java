package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.blocks.machines.EnumMachineType;

@Mixin(value = EnumMachineType.class, remap = false)
public abstract class MixinEnumMachineType {

    @Shadow
    @Final
    private static EnumMachineType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumMachineType[] values() {
        return $VALUES;
    }
}
