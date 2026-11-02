package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.conduit.item.FunctionUpgrade;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FunctionUpgrade.class, remap = false)
public abstract class MixinFunctionUpgrade {

    @Shadow
    @Final
    private static FunctionUpgrade[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FunctionUpgrade[] values() {
        return $VALUES;
    }
}
