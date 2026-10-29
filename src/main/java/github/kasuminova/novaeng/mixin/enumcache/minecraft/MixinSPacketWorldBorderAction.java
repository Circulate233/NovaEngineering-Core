package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.network.play.server.SPacketWorldBorder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SPacketWorldBorder.Action.class, remap = false)
public abstract class MixinSPacketWorldBorderAction {

    @Shadow
    @Final
    private static SPacketWorldBorder.Action[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SPacketWorldBorder.Action[] values() {
        return $VALUES;
    }
}
