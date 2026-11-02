package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.LuminizerVariant;

@Mixin(value = LuminizerVariant.class, remap = false)
public abstract class MixinLuminizerVariant {

    @Shadow
    @Final
    private static LuminizerVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static LuminizerVariant[] values() {
        return $VALUES;
    }
}
