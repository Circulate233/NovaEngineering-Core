package github.kasuminova.novaeng.mixin.enumcache.immersiveengineering;

import blusunrize.immersiveengineering.common.items.ItemRevolver.RevolverPerk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RevolverPerk.class, remap = false)
public abstract class MixinRevolverPerk {

    @Shadow
    @Final
    private static RevolverPerk[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RevolverPerk[] values() {
        return $VALUES;
    }
}
