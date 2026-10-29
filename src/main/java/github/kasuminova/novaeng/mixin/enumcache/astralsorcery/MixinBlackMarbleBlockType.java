package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.common.block.BlockBlackMarble.BlackMarbleBlockType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlackMarbleBlockType.class, remap = false)
public abstract class MixinBlackMarbleBlockType {

    @Shadow
    @Final
    private static BlackMarbleBlockType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BlackMarbleBlockType[] values() {
        return $VALUES;
    }
}
