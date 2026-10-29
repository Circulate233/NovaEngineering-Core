package github.kasuminova.novaeng.mixin.enumcache.cfm;

import com.mrcrayfish.furniture.blocks.BlockChimney.ChimneyType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ChimneyType.class, remap = false)
public abstract class MixinChimneyType {

    @Shadow
    @Final
    private static ChimneyType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ChimneyType[] values() {
        return $VALUES;
    }
}
