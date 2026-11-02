package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.network.play.client.CPacketEntityAction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CPacketEntityAction.Action.class, remap = false)
public abstract class MixinCPacketEntityActionAction {

    @Shadow
    @Final
    private static CPacketEntityAction.Action[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CPacketEntityAction.Action[] values() {
        return $VALUES;
    }
}
