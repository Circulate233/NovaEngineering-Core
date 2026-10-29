package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.gui.dynamic.GuiParser.FluidTankNode.TankType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

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
