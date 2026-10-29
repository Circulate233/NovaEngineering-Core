package github.kasuminova.novaeng.mixin.enumcache.psi;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.psi.api.cad.EnumCADComponent;

@Mixin(value = EnumCADComponent.class, remap = false)
public abstract class MixinEnumCADComponent {

    @Shadow
    @Final
    private static EnumCADComponent[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumCADComponent[] values() {
        return $VALUES;
    }
}
