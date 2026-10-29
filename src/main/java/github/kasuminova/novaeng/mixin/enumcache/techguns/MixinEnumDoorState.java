package github.kasuminova.novaeng.mixin.enumcache.techguns;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import techguns.blocks.EnumDoorState;

@Mixin(value = EnumDoorState.class, remap = false)
public abstract class MixinEnumDoorState {

    @Shadow
    @Final
    private static EnumDoorState[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumDoorState[] values() {
        return $VALUES;
    }
}
