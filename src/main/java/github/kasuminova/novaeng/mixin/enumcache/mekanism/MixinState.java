package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.content.miner.ThreadMinerSearch.State;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = State.class, remap = false)
public abstract class MixinState {

    @Shadow
    @Final
    private static State[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static State[] values() {
        return $VALUES;
    }
}
