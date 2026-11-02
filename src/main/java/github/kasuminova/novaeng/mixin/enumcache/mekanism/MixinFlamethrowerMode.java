package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.item.ItemFlamethrower.FlamethrowerMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FlamethrowerMode.class, remap = false)
public abstract class MixinFlamethrowerMode {

    @Shadow
    @Final
    private static FlamethrowerMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FlamethrowerMode[] values() {
        return $VALUES;
    }
}
