package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.api.state.enums.SpreaderVariant;

@Mixin(value = SpreaderVariant.class, remap = false)
public abstract class MixinSpreaderVariant {

    @Shadow
    @Final
    private static SpreaderVariant[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SpreaderVariant[] values() {
        return $VALUES;
    }
}
