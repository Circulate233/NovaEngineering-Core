package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.powertools.machine.capbank.CapBankType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CapBankType.class, remap = false)
public abstract class MixinCapBankType {

    @Shadow
    @Final
    private static CapBankType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CapBankType[] values() {
        return $VALUES;
    }
}
