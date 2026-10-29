package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.base.IFactory.MachineFuelType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MachineFuelType.class, remap = false)
public abstract class MixinMachineFuelType {

    @Shadow
    @Final
    private static MachineFuelType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MachineFuelType[] values() {
        return $VALUES;
    }
}
