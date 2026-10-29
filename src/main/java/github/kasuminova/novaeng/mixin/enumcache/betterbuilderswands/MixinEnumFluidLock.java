package github.kasuminova.novaeng.mixin.enumcache.betterbuilderswands;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import portablejim.bbw.basics.EnumFluidLock;

@Mixin(value = EnumFluidLock.class, remap = false)
public abstract class MixinEnumFluidLock {

    @Shadow
    @Final
    private static EnumFluidLock[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumFluidLock[] values() {
        return $VALUES;
    }
}
