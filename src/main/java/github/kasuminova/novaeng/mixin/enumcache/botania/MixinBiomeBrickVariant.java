package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.BiomeBrickVariant;

@Mixin(value = BiomeBrickVariant.class, remap = false)
public abstract class MixinBiomeBrickVariant {

    @Shadow
    @Final
    private static BiomeBrickVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BiomeBrickVariant[] values() {
        return $VALUES;
    }
}
