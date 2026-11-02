package github.kasuminova.novaeng.mixin.enumcache.omlib;

import omtteam.omlib.util.EnumMachineMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumMachineMode.class, remap = false)
public abstract class MixinEnumMachineMode {

    @Shadow
    @Final
    private static EnumMachineMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumMachineMode[] values() {
        return $VALUES;
    }
}
