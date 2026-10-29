package github.kasuminova.novaeng.mixin.enumcache.jei;

import mezz.jei.startup.StackHelper.UidMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = UidMode.class, remap = false)
public abstract class MixinUidMode {

    @Shadow
    @Final
    private static UidMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static UidMode[] values() {
        return $VALUES;
    }
}
