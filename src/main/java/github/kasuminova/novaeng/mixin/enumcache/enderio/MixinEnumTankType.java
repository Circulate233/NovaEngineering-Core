package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.machines.machine.tank.EnumTankType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumTankType.class, remap = false)
public abstract class MixinEnumTankType {

    @Shadow
    @Final
    private static EnumTankType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumTankType[] values() {
        return $VALUES;
    }
}
