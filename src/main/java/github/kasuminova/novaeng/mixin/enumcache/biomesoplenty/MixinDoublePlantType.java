package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.common.block.BlockBOPDoublePlant.DoublePlantType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DoublePlantType.class, remap = false)
public abstract class MixinDoublePlantType {

    @Shadow
    @Final
    private static DoublePlantType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DoublePlantType[] values() {
        return $VALUES;
    }
}
