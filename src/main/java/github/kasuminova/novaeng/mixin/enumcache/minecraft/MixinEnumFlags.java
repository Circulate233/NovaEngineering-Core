package github.kasuminova.novaeng.mixin.enumcache.minecraft;

import net.minecraft.network.play.server.SPacketPlayerPosLook;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SPacketPlayerPosLook.EnumFlags.class, remap = false)
public abstract class MixinEnumFlags {

    @Shadow
    @Final
    private static SPacketPlayerPosLook.EnumFlags[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SPacketPlayerPosLook.EnumFlags[] values() {
        return $VALUES;
    }
}
