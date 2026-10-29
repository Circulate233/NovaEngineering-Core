package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.items.modifier.ModifierFilterOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModifierFilterOperation.class, remap = false)
public abstract class MixinModifierFilterOperation {

    @Shadow
    @Final
    private static ModifierFilterOperation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ModifierFilterOperation[] values() {
        return $VALUES;
    }
}
