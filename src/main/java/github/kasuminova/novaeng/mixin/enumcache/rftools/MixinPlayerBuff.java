package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.PlayerBuff;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PlayerBuff.class, remap = false)
public abstract class MixinPlayerBuff {

    @Shadow
    @Final
    private static PlayerBuff[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PlayerBuff[] values() {
        return $VALUES;
    }
}
