package github.kasuminova.novaeng.mixin.enumcache.chisel;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import team.chisel.api.IMC;

@Mixin(value = IMC.class, remap = false)
public abstract class MixinIMC {

    @Shadow
    @Final
    private static IMC[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static IMC[] values() {
        return $VALUES;
    }
}
