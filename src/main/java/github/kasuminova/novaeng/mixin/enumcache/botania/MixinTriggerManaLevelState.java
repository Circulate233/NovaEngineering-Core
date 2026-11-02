package github.kasuminova.novaeng.mixin.enumcache.botania;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import vazkii.botania.common.integration.buildcraft.TriggerManaLevel.State;

@Mixin(value = State.class, remap = false)
public abstract class MixinTriggerManaLevelState {

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
