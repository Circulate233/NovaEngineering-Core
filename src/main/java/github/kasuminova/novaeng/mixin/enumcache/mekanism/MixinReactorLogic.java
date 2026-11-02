package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.generators.common.tile.reactor.TileEntityReactorLogicAdapter.ReactorLogic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ReactorLogic.class, remap = false)
public abstract class MixinReactorLogic {

    @Shadow
    @Final
    private static ReactorLogic[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ReactorLogic[] values() {
        return $VALUES;
    }
}
