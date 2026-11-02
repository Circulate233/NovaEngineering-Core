package github.kasuminova.novaeng.mixin.enumcache.chisel;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import team.chisel.common.item.ItemChisel.ChiselType;

@Mixin(value = ChiselType.class, remap = false)
public abstract class MixinChiselType {

    @Shadow
    @Final
    private static ChiselType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ChiselType[] values() {
        return $VALUES;
    }
}
