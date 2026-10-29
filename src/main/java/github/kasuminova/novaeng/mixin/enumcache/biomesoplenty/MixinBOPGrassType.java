package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.common.block.BlockBOPGrass.BOPGrassType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BOPGrassType.class, remap = false)
public abstract class MixinBOPGrassType {

    @Shadow
    @Final
    private static BOPGrassType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BOPGrassType[] values() {
        return $VALUES;
    }
}
