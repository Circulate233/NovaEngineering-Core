package github.kasuminova.novaeng.mixin.enumcache.advancedrocketry;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zmaster587.advancedRocketry.block.BlockOxidizerFuelTank.TankStates;

@Mixin(value = TankStates.class, remap = false)
public abstract class MixinBlockOxidizerFuelTankTankStates {

    @Shadow
    @Final
    private static TankStates[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TankStates[] values() {
        return $VALUES;
    }
}
