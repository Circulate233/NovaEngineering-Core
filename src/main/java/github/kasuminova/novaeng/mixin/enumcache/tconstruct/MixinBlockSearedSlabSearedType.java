package github.kasuminova.novaeng.mixin.enumcache.tconstruct;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.tconstruct.smeltery.block.BlockSearedSlab.SearedType;

@Mixin(value = SearedType.class, remap = false)
public abstract class MixinBlockSearedSlabSearedType {

    @Shadow
    @Final
    private static SearedType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SearedType[] values() {
        return $VALUES;
    }
}
