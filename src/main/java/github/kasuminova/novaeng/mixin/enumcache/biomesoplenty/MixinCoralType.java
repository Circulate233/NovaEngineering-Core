package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.common.block.BlockBOPCoral.CoralType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CoralType.class, remap = false)
public abstract class MixinCoralType {

    @Shadow
    @Final
    private static CoralType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CoralType[] values() {
        return $VALUES;
    }
}
