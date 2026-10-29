package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.conduit.facade.EnumFacadeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumFacadeType.class, remap = false)
public abstract class MixinEnumFacadeType {

    @Shadow
    @Final
    private static EnumFacadeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumFacadeType[] values() {
        return $VALUES;
    }
}
