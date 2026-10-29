package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.network.play.server.SPacketUpdateScore;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SPacketUpdateScore.Action.class, remap = false)
public abstract class MixinSPacketUpdateScoreAction {

    @Shadow
    @Final
    private static SPacketUpdateScore.Action[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SPacketUpdateScore.Action[] values() {
        return $VALUES;
    }
}
