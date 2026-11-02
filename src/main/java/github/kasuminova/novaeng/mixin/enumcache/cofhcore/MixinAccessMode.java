package github.kasuminova.novaeng.mixin.enumcache.cofhcore;

import cofh.api.core.ISecurable.AccessMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AccessMode.class, remap = false)
public abstract class MixinAccessMode {

    @Shadow
    @Final
    private static AccessMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AccessMode[] values() {
        return $VALUES;
    }
}
