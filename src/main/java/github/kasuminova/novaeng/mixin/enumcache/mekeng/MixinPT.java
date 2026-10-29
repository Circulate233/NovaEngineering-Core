package github.kasuminova.novaeng.mixin.enumcache.mekeng;

import com.mekeng.github.network.packet.sync.ParaSerializer.PT;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PT.class, remap = false)
public abstract class MixinPT {

    @Shadow
    @Final
    private static PT[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PT[] values() {
        return $VALUES;
    }
}
