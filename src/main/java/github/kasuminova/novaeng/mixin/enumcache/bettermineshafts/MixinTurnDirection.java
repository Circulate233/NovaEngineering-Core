package github.kasuminova.novaeng.mixin.enumcache.bettermineshafts;

import com.yungnickyoung.minecraft.bettermineshafts.world.generator.pieces.SmallTunnelTurn.TurnDirection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TurnDirection.class, remap = false)
public abstract class MixinTurnDirection {

    @Shadow
    @Final
    private static TurnDirection[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TurnDirection[] values() {
        return $VALUES;
    }
}
