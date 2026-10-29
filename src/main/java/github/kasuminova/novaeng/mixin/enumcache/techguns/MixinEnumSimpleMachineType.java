package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.blocks.machines.EnumSimpleMachineType;

@Mixin(value = EnumSimpleMachineType.class, remap = false)
public abstract class MixinEnumSimpleMachineType {

    @Shadow
    @Final
    private static EnumSimpleMachineType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumSimpleMachineType[] values() {
        return $VALUES;
    }
}
