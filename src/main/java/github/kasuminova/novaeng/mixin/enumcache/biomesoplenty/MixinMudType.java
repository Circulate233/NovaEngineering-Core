package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.common.block.BlockBOPMud.MudType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MudType.class, remap = false)
public abstract class MixinMudType {

    @Shadow
    @Final
    private static MudType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MudType[] values() {
        return $VALUES;
    }
}
