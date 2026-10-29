package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.content.gear.mekatool.ModuleBlastingUnit.BlastRadius;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlastRadius.class, remap = false)
public abstract class MixinBlastRadius {

    @Shadow
    @Final
    private static BlastRadius[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlastRadius[] values() {
        return $VALUES;
    }
}
