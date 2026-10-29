package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.tools.common.Materials;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Materials.class, remap = false)
public abstract class MixinMaterials {

    @Shadow
    @Final
    private static Materials[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Materials[] values() {
        return $VALUES;
    }
}
