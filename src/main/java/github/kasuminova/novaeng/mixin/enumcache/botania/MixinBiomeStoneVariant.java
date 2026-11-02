package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.BiomeStoneVariant;

@Mixin(value = BiomeStoneVariant.class, remap = false)
public abstract class MixinBiomeStoneVariant {

    @Shadow
    @Final
    private static BiomeStoneVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BiomeStoneVariant[] values() {
        return $VALUES;
    }
}
