package github.kasuminova.novaeng.mixin.enumcache.ancientspellcraft;

import com.windanesz.ancientspellcraft.packet.PacketActivateBauble.ControlType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ControlType.class, remap = false)
public abstract class MixinPacketActivateBaubleControlType {

    @Shadow
    @Final
    private static ControlType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ControlType[] values() {
        return $VALUES;
    }
}
