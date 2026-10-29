package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.gadgets.item.ItemThrowball.ThrowballType;

@Mixin(value = ThrowballType.class, remap = false)
public abstract class MixinThrowballType {

    @Shadow
    @Final
    private static ThrowballType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ThrowballType[] values() {
        return $VALUES;
    }
}
