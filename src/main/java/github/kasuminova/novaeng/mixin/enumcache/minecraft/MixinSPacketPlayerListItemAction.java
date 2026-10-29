package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.network.play.server.SPacketPlayerListItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SPacketPlayerListItem.Action.class, remap = false)
public abstract class MixinSPacketPlayerListItemAction {

    @Shadow
    @Final
    private static SPacketPlayerListItem.Action[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SPacketPlayerListItem.Action[] values() {
        return $VALUES;
    }
}
