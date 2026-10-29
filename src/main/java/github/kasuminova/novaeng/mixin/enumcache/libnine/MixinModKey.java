package github.kasuminova.novaeng.mixin.enumcache.libnine;

import io.github.phantamanta44.libnine.util.helper.InputUtils.ModKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModKey.class, remap = false)
public abstract class MixinModKey {

    @Shadow
    @Final
    private static ModKey[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModKey[] values() {
        return $VALUES;
    }
}
