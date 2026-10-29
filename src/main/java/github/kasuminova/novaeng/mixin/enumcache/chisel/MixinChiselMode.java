package github.kasuminova.novaeng.mixin.enumcache.chisel;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import team.chisel.common.item.ChiselMode;

@Mixin(value = ChiselMode.class, remap = false)
public abstract class MixinChiselMode {

    @Shadow
    @Final
    private static ChiselMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ChiselMode[] values() {
        return $VALUES;
    }
}
