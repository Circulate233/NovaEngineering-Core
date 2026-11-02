package github.kasuminova.novaeng.mixin.enumcache.betterbuilderswands;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import portablejim.bbw.basics.EnumLock;

@Mixin(value = EnumLock.class, remap = false)
public abstract class MixinEnumLock {

    @Shadow
    @Final
    private static EnumLock[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumLock[] values() {
        return $VALUES;
    }
}
