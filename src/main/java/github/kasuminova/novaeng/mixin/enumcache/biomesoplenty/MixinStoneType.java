package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.common.block.BlockBOPWhiteSandstone.StoneType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = StoneType.class, remap = false)
public abstract class MixinStoneType {

    @Shadow
    @Final
    private static StoneType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static StoneType[] values() {
        return $VALUES;
    }
}
