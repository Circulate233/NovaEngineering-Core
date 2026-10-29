package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.network.play.client.CPacketResourcePackStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CPacketResourcePackStatus.Action.class, remap = false)
public abstract class MixinCPacketResourcePackStatusAction {

    @Shadow
    @Final
    private static CPacketResourcePackStatus.Action[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CPacketResourcePackStatus.Action[] values() {
        return $VALUES;
    }
}
