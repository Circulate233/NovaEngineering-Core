package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.network.play.server.SPacketUpdateBossInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SPacketUpdateBossInfo.Operation.class, remap = false)
public abstract class MixinSPacketUpdateBossInfoOperation {

    @Shadow
    @Final
    private static SPacketUpdateBossInfo.Operation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SPacketUpdateBossInfo.Operation[] values() {
        return $VALUES;
    }
}
