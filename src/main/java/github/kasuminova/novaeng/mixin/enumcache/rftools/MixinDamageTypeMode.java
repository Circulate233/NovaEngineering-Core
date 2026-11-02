package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.blocks.shield.DamageTypeMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DamageTypeMode.class, remap = false)
public abstract class MixinDamageTypeMode {

    @Shadow
    @Final
    private static DamageTypeMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DamageTypeMode[] values() {
        return $VALUES;
    }
}
