package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.common.network.PacketBotaniaEffect.EffectType;

@Mixin(value = EffectType.class, remap = false)
public abstract class MixinEffectType {

    @Shadow
    @Final
    private static EffectType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EffectType[] values() {
        return $VALUES;
    }
}
