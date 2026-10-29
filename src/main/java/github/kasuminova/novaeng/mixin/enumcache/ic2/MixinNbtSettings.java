package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.item.upgrade.NbtSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NbtSettings.class, remap = false)
public abstract class MixinNbtSettings {

    @Shadow
    @Final
    private static NbtSettings[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static NbtSettings[] values() {
        return $VALUES;
    }
}
