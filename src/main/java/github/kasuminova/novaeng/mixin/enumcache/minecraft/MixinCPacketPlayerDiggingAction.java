package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.network.play.client.CPacketPlayerDigging;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CPacketPlayerDigging.Action.class, remap = false)
public abstract class MixinCPacketPlayerDiggingAction {

    @Shadow
    @Final
    private static CPacketPlayerDigging.Action[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CPacketPlayerDigging.Action[] values() {
        return $VALUES;
    }
}
