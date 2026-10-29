package github.kasuminova.novaeng.mixin.enumcache.immersiveengineering;

import blusunrize.immersiveengineering.api.tool.ConveyorHandler.ConveyorDirection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConveyorDirection.class, remap = false)
public abstract class MixinConveyorDirection {

    @Shadow
    @Final
    private static ConveyorDirection[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ConveyorDirection[] values() {
        return $VALUES;
    }
}
