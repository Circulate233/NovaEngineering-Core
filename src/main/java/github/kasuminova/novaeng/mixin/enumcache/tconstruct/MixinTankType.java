package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.smeltery.block.BlockTank.TankType;

@Mixin(value = TankType.class, remap = false)
public abstract class MixinTankType {

    @Shadow
    @Final
    private static TankType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TankType[] values() {
        return $VALUES;
    }
}
