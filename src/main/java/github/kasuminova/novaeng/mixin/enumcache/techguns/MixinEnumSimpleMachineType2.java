package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.blocks.machines.EnumSimpleMachineType2;

@Mixin(value = EnumSimpleMachineType2.class, remap = false)
public abstract class MixinEnumSimpleMachineType2 {

    @Shadow
    @Final
    private static EnumSimpleMachineType2[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumSimpleMachineType2[] values() {
        return $VALUES;
    }
}
