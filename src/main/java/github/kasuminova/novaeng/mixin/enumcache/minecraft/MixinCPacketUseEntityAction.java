package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.network.play.client.CPacketUseEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CPacketUseEntity.Action.class, remap = false)
public abstract class MixinCPacketUseEntityAction {

    @Shadow
    @Final
    private static CPacketUseEntity.Action[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CPacketUseEntity.Action[] values() {
        return $VALUES;
    }
}
