package github.kasuminova.novaeng.mixin.enumcache.tconevo;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xyz.phanta.tconevo.block.BlockEarthMaterial.Type;

@Mixin(value = Type.class, remap = false)
public abstract class MixinBlockEarthMaterialType {

    @Shadow
    @Final
    private static Type[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Type[] values() {
        return $VALUES;
    }
}
