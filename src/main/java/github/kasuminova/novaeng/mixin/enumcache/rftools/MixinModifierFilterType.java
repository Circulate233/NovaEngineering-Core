package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.items.modifier.ModifierFilterType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModifierFilterType.class, remap = false)
public abstract class MixinModifierFilterType {

    @Shadow
    @Final
    private static ModifierFilterType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModifierFilterType[] values() {
        return $VALUES;
    }
}
