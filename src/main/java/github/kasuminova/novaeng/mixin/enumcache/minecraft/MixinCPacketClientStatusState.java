package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.network.play.client.CPacketClientStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CPacketClientStatus.State.class, remap = false)
public abstract class MixinCPacketClientStatusState {

    @Shadow
    @Final
    private static CPacketClientStatus.State[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CPacketClientStatus.State[] values() {
        return $VALUES;
    }
}
