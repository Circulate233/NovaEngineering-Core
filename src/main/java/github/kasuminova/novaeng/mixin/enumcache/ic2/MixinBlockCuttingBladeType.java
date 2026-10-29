package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.item.type.BlockCuttingBladeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockCuttingBladeType.class, remap = false)
public abstract class MixinBlockCuttingBladeType {

    @Shadow
    @Final
    private static BlockCuttingBladeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlockCuttingBladeType[] values() {
        return $VALUES;
    }
}
